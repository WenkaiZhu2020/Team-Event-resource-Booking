package com.teamresource.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.notification.api.dto.NotificationResponse;
import com.teamresource.notification.api.dto.UnreadCountResponse;
import com.teamresource.notification.domain.NotificationChannel;
import com.teamresource.notification.domain.NotificationStatus;
import com.teamresource.notification.domain.repository.DeliveryAttemptRepository;
import com.teamresource.notification.domain.repository.NotificationRepository;
import com.teamresource.notification.infra.client.EventReminderCandidate;
import com.teamresource.notification.infra.messaging.BookingEventPayload;
import com.teamresource.notification.infra.messaging.DomainEventMessage;
import com.teamresource.notification.infra.persistence.NotificationRecordEntity;
import com.teamresource.notification.infra.persistence.NotificationRecordRepository;
import com.teamresource.notification.infra.persistence.ProcessedEventEntity;
import com.teamresource.notification.infra.persistence.ProcessedEventRepository;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationDeliveryAttemptEntity;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import com.teamresource.notification.service.channel.NotificationSenderFactory;
import com.teamresource.notification.service.template.EventReminderTemplateRenderer;
import com.teamresource.notification.service.template.NotificationTemplateRegistry;
import com.teamresource.notification.service.template.RenderedNotification;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRecordRepository notificationRecordRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final NotificationSenderFactory notificationSenderFactory;
    private final NotificationTemplateRegistry templateRegistry;
    private final EventReminderTemplateRenderer eventReminderTemplateRenderer;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;

    public NotificationService(
            NotificationRecordRepository notificationRecordRepository,
            ProcessedEventRepository processedEventRepository,
            NotificationSenderFactory notificationSenderFactory,
            NotificationTemplateRegistry templateRegistry,
            EventReminderTemplateRenderer eventReminderTemplateRenderer,
            ObjectMapper objectMapper
    ) {
        this(
                notificationRecordRepository,
                processedEventRepository,
                notificationSenderFactory,
                templateRegistry,
                eventReminderTemplateRenderer,
                objectMapper,
                null,
                null
        );
    }

    @Autowired
    public NotificationService(
            NotificationRecordRepository notificationRecordRepository,
            ProcessedEventRepository processedEventRepository,
            NotificationSenderFactory notificationSenderFactory,
            NotificationTemplateRegistry templateRegistry,
            EventReminderTemplateRenderer eventReminderTemplateRenderer,
            ObjectMapper objectMapper,
            NotificationRepository notificationRepository,
            DeliveryAttemptRepository deliveryAttemptRepository
    ) {
        this.notificationRecordRepository = notificationRecordRepository;
        this.processedEventRepository = processedEventRepository;
        this.notificationSenderFactory = notificationSenderFactory;
        this.templateRegistry = templateRegistry;
        this.eventReminderTemplateRenderer = eventReminderTemplateRenderer;
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
    }

    @Transactional
    public void consume(DomainEventMessage eventMessage) {
        if (!markProcessed(eventMessage.messageId(), eventMessage.eventType(), eventMessage.aggregateId())) {
            return;
        }

        BookingEventPayload payload = objectMapper.convertValue(eventMessage.payload(), BookingEventPayload.class);
        RenderedNotification rendered = templateRegistry.resolve(eventMessage.eventType()).render(payload);
        createAndSend(eventMessage.messageId(), eventMessage.eventType(), payload.userId(), rendered);
    }

    @Transactional
    public void sendEventReminder(EventReminderCandidate reminder) {
        UUID processedId = UUID.nameUUIDFromBytes(
                ("event.reminder:" + reminder.registrationId()).getBytes(StandardCharsets.UTF_8)
        );
        if (!markProcessed(processedId, "event.reminder", reminder.registrationId())) {
            return;
        }

        RenderedNotification rendered = eventReminderTemplateRenderer.render(reminder);
        createAndSend(reminder.eventId(), "event.reminder", reminder.userId(), rendered);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications(UUID userId, String channel) {
        List<NotificationRecordEntity> records;
        if (channel == null || channel.isBlank()) {
            records = notificationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else {
            records = notificationRecordRepository.findByUserIdAndChannelOrderByCreatedAtDesc(userId, parseChannel(channel));
        }
        return records.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRecordRepository.countByUserIdAndStatusNot(userId, NotificationStatus.READ));
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId, UUID userId) {
        NotificationRecordEntity record = notificationRecordRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!record.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification access denied");
        }
        record.setStatus(NotificationStatus.READ);
        record.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
        record.setUpdatedAt(record.getReadAt());
        syncReadState(record);
        return toResponse(notificationRecordRepository.save(record));
    }

    private boolean markProcessed(UUID processedEventId, String eventType, UUID aggregateId) {
        try {
            ProcessedEventEntity processedEvent = new ProcessedEventEntity();
            processedEvent.setProcessedEventId(processedEventId);
            processedEvent.setEventType(eventType);
            processedEvent.setAggregateId(aggregateId);
            processedEvent.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            processedEventRepository.save(processedEvent);
            return true;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }

    private void createAndSend(UUID sourceEventId, String sourceEventType, UUID userId, RenderedNotification rendered) {
        createAndSend(sourceEventId, sourceEventType, userId, NotificationChannel.IN_APP, rendered);
        createAndSend(sourceEventId, sourceEventType, userId, NotificationChannel.EMAIL, rendered);
    }

    private void createAndSend(
            UUID sourceEventId,
            String sourceEventType,
            UUID userId,
            NotificationChannel channel,
            RenderedNotification rendered
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UUID notificationId = UUID.randomUUID();
        NotificationRecordEntity record = new NotificationRecordEntity();
        record.setNotificationId(notificationId);
        record.setUserId(userId);
        record.setSourceEventId(sourceEventId);
        record.setSourceEventType(sourceEventType);
        record.setNotificationType(rendered.type());
        record.setChannel(channel);
        record.setSubject(rendered.subject());
        record.setBody(rendered.body());
        record.setStatus(NotificationStatus.FAILED);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        NotificationEntity enhanced = buildEnhancedRecord(record, sourceEventType, now);

        try {
            notificationSenderFactory.get(channel).send(record);
            notificationRecordRepository.save(record);
            syncEnhancedRecord(enhanced, record, now, true);
        } catch (Exception ex) {
            record.setFailureReason(ex.getMessage());
            notificationRecordRepository.save(record);
            syncEnhancedRecord(enhanced, record, now, false);
        }
    }

    private NotificationEntity buildEnhancedRecord(NotificationRecordEntity record, String sourceEventType, OffsetDateTime now) {
        if (notificationRepository == null) {
            return null;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setId(record.getNotificationId());
        entity.setUserId(record.getUserId());
        entity.setType(mapType(record.getNotificationType()));
        entity.setChannel(com.teamresource.notification.domain.model.NotificationChannel.valueOf(record.getChannel().name()));
        entity.setStatus(com.teamresource.notification.domain.model.NotificationStatus.PENDING);
        entity.setTitle(record.getSubject());
        entity.setBody(record.getBody());
        entity.setTemplateCode(entity.getType().name());
        entity.setSource(sourceEventType != null && sourceEventType.startsWith("booking.") ? "booking-domain" : "system");
        entity.setSourceEventType(sourceEventType);
        entity.setReferenceType(sourceEventType != null && sourceEventType.startsWith("booking.") ? "BOOKING" : "EVENT");
        entity.setReferenceId(record.getSourceEventId());
        entity.setIdempotencyKey(record.getSourceEventId() + ":" + record.getUserId() + ":" + record.getChannel() + ":" + entity.getType().name());
        entity.setRetryCount(0);
        entity.setMaxRetries(3);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private void syncEnhancedRecord(NotificationEntity enhanced, NotificationRecordEntity record, OffsetDateTime now, boolean success) {
        if (notificationRepository == null || deliveryAttemptRepository == null || enhanced == null) {
            return;
        }
        enhanced.setStatus(success
                ? com.teamresource.notification.domain.model.NotificationStatus.SENT
                : com.teamresource.notification.domain.model.NotificationStatus.FAILED);
        enhanced.setSentAt(success ? record.getSentAt() : null);
        enhanced.setReadAt(record.getReadAt());
        enhanced.setLastError(record.getFailureReason());
        enhanced.setUpdatedAt(now);
        notificationRepository.save(enhanced);

        NotificationDeliveryAttemptEntity attempt = new NotificationDeliveryAttemptEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setNotificationId(enhanced.getId());
        attempt.setChannel(com.teamresource.notification.domain.model.NotificationChannel.valueOf(record.getChannel().name()));
        attempt.setAttemptNo(1);
        attempt.setStatus(success
                ? com.teamresource.notification.domain.model.DeliveryAttemptStatus.SUCCESS
                : com.teamresource.notification.domain.model.DeliveryAttemptStatus.FAILED);
        attempt.setProviderMessageId(success ? enhanced.getId().toString() : null);
        attempt.setErrorMessage(record.getFailureReason());
        attempt.setAttemptedAt(now);
        attempt.setDurationMs(1L);
        deliveryAttemptRepository.save(attempt);
    }

    private void syncReadState(NotificationRecordEntity record) {
        if (notificationRepository == null) {
            return;
        }
        notificationRepository.findById(record.getNotificationId()).ifPresent(entity -> {
            entity.setStatus(com.teamresource.notification.domain.model.NotificationStatus.READ);
            entity.setReadAt(record.getReadAt());
            entity.setUpdatedAt(record.getUpdatedAt());
            notificationRepository.save(entity);
        });
    }

    private com.teamresource.notification.domain.model.NotificationType mapType(com.teamresource.notification.domain.NotificationType type) {
        return switch (type) {
            case BOOKING_CREATED -> com.teamresource.notification.domain.model.NotificationType.BOOKING_CONFIRMED;
            case BOOKING_APPROVED -> com.teamresource.notification.domain.model.NotificationType.BOOKING_APPROVED;
            case BOOKING_REJECTED -> com.teamresource.notification.domain.model.NotificationType.BOOKING_REJECTED;
            case BOOKING_CANCELLED -> com.teamresource.notification.domain.model.NotificationType.BOOKING_CANCELLED;
            case WAITLIST_PROMOTED -> com.teamresource.notification.domain.model.NotificationType.WAITLIST_PROMOTED;
            case EVENT_REMINDER -> com.teamresource.notification.domain.model.NotificationType.EVENT_REMINDER;
        };
    }

    private NotificationChannel parseChannel(String raw) {
        try {
            return NotificationChannel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification channel");
        }
    }

    private NotificationResponse toResponse(NotificationRecordEntity record) {
        return new NotificationResponse(
                record.getNotificationId(),
                record.getUserId(),
                record.getSourceEventId(),
                record.getSourceEventType(),
                record.getNotificationType().name(),
                record.getChannel().name(),
                record.getSubject(),
                record.getBody(),
                record.getStatus().name(),
                record.getReadAt(),
                record.getSentAt(),
                record.getFailureReason(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
