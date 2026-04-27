package com.teamresource.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.notification.api.dto.NotificationResponse;
import com.teamresource.notification.api.dto.UnreadCountResponse;
import com.teamresource.notification.domain.NotificationChannel;
import com.teamresource.notification.domain.NotificationStatus;
import com.teamresource.notification.infra.messaging.BookingEventPayload;
import com.teamresource.notification.infra.messaging.DomainEventMessage;
import com.teamresource.notification.infra.persistence.NotificationRecordEntity;
import com.teamresource.notification.infra.persistence.NotificationRecordRepository;
import com.teamresource.notification.infra.persistence.ProcessedEventEntity;
import com.teamresource.notification.infra.persistence.ProcessedEventRepository;
import com.teamresource.notification.service.channel.NotificationSenderFactory;
import com.teamresource.notification.service.template.NotificationTemplateRegistry;
import com.teamresource.notification.service.template.RenderedNotification;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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
    private final ObjectMapper objectMapper;

    public NotificationService(
            NotificationRecordRepository notificationRecordRepository,
            ProcessedEventRepository processedEventRepository,
            NotificationSenderFactory notificationSenderFactory,
            NotificationTemplateRegistry templateRegistry,
            ObjectMapper objectMapper
    ) {
        this.notificationRecordRepository = notificationRecordRepository;
        this.processedEventRepository = processedEventRepository;
        this.notificationSenderFactory = notificationSenderFactory;
        this.templateRegistry = templateRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void consume(DomainEventMessage eventMessage) {
        try {
            ProcessedEventEntity processedEvent = new ProcessedEventEntity();
            processedEvent.setProcessedEventId(eventMessage.messageId());
            processedEvent.setEventType(eventMessage.eventType());
            processedEvent.setAggregateId(eventMessage.aggregateId());
            processedEvent.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
            processedEventRepository.save(processedEvent);
        } catch (DataIntegrityViolationException ignored) {
            return;
        }

        BookingEventPayload payload = objectMapper.convertValue(eventMessage.payload(), BookingEventPayload.class);
        RenderedNotification rendered = templateRegistry.resolve(eventMessage.eventType()).render(payload);

        createAndSend(eventMessage, payload.userId(), NotificationChannel.IN_APP, rendered);
        createAndSend(eventMessage, payload.userId(), NotificationChannel.EMAIL, rendered);
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
        return toResponse(notificationRecordRepository.save(record));
    }

    private void createAndSend(
            DomainEventMessage eventMessage,
            UUID userId,
            NotificationChannel channel,
            RenderedNotification rendered
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        NotificationRecordEntity record = new NotificationRecordEntity();
        record.setNotificationId(UUID.randomUUID());
        record.setUserId(userId);
        record.setSourceEventId(eventMessage.messageId());
        record.setSourceEventType(eventMessage.eventType());
        record.setNotificationType(rendered.type());
        record.setChannel(channel);
        record.setSubject(rendered.subject());
        record.setBody(rendered.body());
        record.setStatus(NotificationStatus.FAILED);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        try {
            notificationSenderFactory.get(channel).send(record);
            notificationRecordRepository.save(record);
        } catch (Exception ex) {
            record.setFailureReason(ex.getMessage());
            notificationRecordRepository.save(record);
        }
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
