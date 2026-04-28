package com.teamresource.notification.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.notification.application.factory.ChannelSenderFactory;
import com.teamresource.notification.application.pipeline.ChannelSendResult;
import com.teamresource.notification.application.pipeline.NotificationDispatchCommand;
import com.teamresource.notification.application.pipeline.NotificationDispatchContext;
import com.teamresource.notification.domain.model.DeliveryAttemptStatus;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.repository.DeliveryAttemptRepository;
import com.teamresource.notification.domain.repository.NotificationRepository;
import com.teamresource.notification.infrastructure.config.MessagingProperties;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationDeliveryAttemptEntity;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDispatchService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final NotificationRepository notificationRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateService templateService;
    private final ChannelSenderFactory channelSenderFactory;
    private final MessagingProperties messagingProperties;
    private final ObjectMapper objectMapper;

    public NotificationDispatchService(
            NotificationRepository notificationRepository,
            DeliveryAttemptRepository deliveryAttemptRepository,
            NotificationPreferenceService preferenceService,
            NotificationTemplateService templateService,
            ChannelSenderFactory channelSenderFactory,
            MessagingProperties messagingProperties,
            ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.preferenceService = preferenceService;
        this.templateService = templateService;
        this.channelSenderFactory = channelSenderFactory;
        this.messagingProperties = messagingProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<NotificationEntity> dispatch(NotificationDispatchCommand command) {
        List<NotificationEntity> created = new ArrayList<>();
        NotificationPreferenceEntity preference = preferenceService.getOrCreate(command.userId());

        for (NotificationChannel channel : command.channels()) {
            NotificationEntity entity = createOrLoad(command, channel, preference);
            created.add(entity);

            if (entity.getStatus() == NotificationStatus.SKIPPED) {
                continue;
            }

            if (entity.getScheduledAt() != null && entity.getScheduledAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
                continue;
            }

            sendNow(entity, command.templateData());
        }

        return created;
    }

    @Transactional
    public int retryFailed(int limit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<NotificationEntity> retryable = notificationRepository.findRetryableFailed(NotificationStatus.FAILED, now, limit);
        for (NotificationEntity notification : retryable) {
            sendNow(notification, readMetadata(notification));
        }
        return retryable.size();
    }

    @Transactional
    public int sendDueReminders(int limit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<NotificationEntity> due = notificationRepository.findScheduledDue(NotificationStatus.PENDING, now, limit);
        for (NotificationEntity notification : due) {
            sendNow(notification, readMetadata(notification));
        }
        return due.size();
    }

    private NotificationEntity createOrLoad(
            NotificationDispatchCommand command,
            NotificationChannel channel,
            NotificationPreferenceEntity preference
    ) {
        String idempotencyKey = command.idempotencyKey() + ":" + channel.name();
        NotificationEntity existing = notificationRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return existing;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RenderedTemplate rendered = templateService.render(command.templateCode(), channel, command.templateData());

        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setUserId(command.userId());
        notification.setType(command.type());
        notification.setChannel(channel);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setTitle(rendered.title());
        notification.setBody(rendered.body());
        notification.setTemplateCode(command.templateCode());
        notification.setSource(command.source());
        notification.setSourceEventType(command.sourceEventType());
        notification.setReferenceType(command.referenceType());
        notification.setReferenceId(command.referenceId());
        notification.setIdempotencyKey(idempotencyKey);
        notification.setMetadataJson(writeMetadata(command.templateData()));
        notification.setScheduledAt(resolveScheduledAt(command, preference));
        notification.setRetryCount(0);
        notification.setMaxRetries(command.maxRetries() == null
                ? Math.max(1, messagingProperties.retry().maxAttempts())
                : Math.max(1, command.maxRetries()));
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);

        if (!channelEnabled(channel, preference)) {
            notification.setStatus(NotificationStatus.SKIPPED);
            notification.setSentAt(now);
            notification.setLastError("Channel disabled by user preference");
            NotificationEntity skipped = notificationRepository.save(notification);
            saveAttempt(skipped, null, DeliveryAttemptStatus.SKIPPED, 1, "Channel disabled by user preference", 1);
            return skipped;
        }

        return notificationRepository.save(notification);
    }

    private OffsetDateTime resolveScheduledAt(NotificationDispatchCommand command, NotificationPreferenceEntity preference) {
        if (command.scheduledAt() == null) {
            return null;
        }
        if (command.type() == com.teamresource.notification.domain.model.NotificationType.EVENT_REMINDER) {
            Object startAtValue = command.templateData() == null ? null : command.templateData().get("startAt");
            OffsetDateTime startAt = parseTime(startAtValue);
            if (startAt != null) {
                return startAt.minusMinutes(Math.max(5, preference.getReminderLeadMinutes()));
            }
        }
        return command.scheduledAt();
    }

    private boolean channelEnabled(NotificationChannel channel, NotificationPreferenceEntity preference) {
        return switch (channel) {
            case IN_APP -> preference.isInAppEnabled();
            case EMAIL -> preference.isEmailEnabled();
        };
    }

    private void sendNow(NotificationEntity notification, Map<String, Object> templateData) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        NotificationDispatchContext context = new NotificationDispatchContext(notification, templateData);

        ChannelSendResult result = channelSenderFactory.get(notification.getChannel()).send(context);

        int attemptNo = notification.getRetryCount() + 1;
        if (result.success()) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(now);
            notification.setLastError(null);
            notification.setNextRetryAt(null);
            notification.setUpdatedAt(now);
            notificationRepository.save(notification);
            saveAttempt(notification, result.providerMessageId(), DeliveryAttemptStatus.SUCCESS, attemptNo, null, result.durationMs());
            return;
        }

        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setLastError(result.errorMessage());
        notification.setUpdatedAt(now);

        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            notification.setNextRetryAt(null);
        } else {
            notification.setNextRetryAt(now.plusSeconds(Math.max(1, messagingProperties.retry().backoffSeconds())));
        }

        notificationRepository.save(notification);
        saveAttempt(notification, null, DeliveryAttemptStatus.FAILED, attemptNo, result.errorMessage(), result.durationMs());
    }

    private void saveAttempt(
            NotificationEntity notification,
            String providerMessageId,
            DeliveryAttemptStatus status,
            int attemptNo,
            String errorMessage,
            long durationMs
    ) {
        NotificationDeliveryAttemptEntity attempt = new NotificationDeliveryAttemptEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setNotificationId(notification.getId());
        attempt.setChannel(notification.getChannel());
        attempt.setAttemptNo(attemptNo);
        attempt.setStatus(status);
        attempt.setProviderMessageId(providerMessageId);
        attempt.setErrorMessage(errorMessage);
        attempt.setAttemptedAt(OffsetDateTime.now(ZoneOffset.UTC));
        attempt.setDurationMs(Math.max(1L, durationMs));
        deliveryAttemptRepository.save(attempt);
    }

    private String writeMetadata(Map<String, Object> templateData) {
        if (templateData == null || templateData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(templateData);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private Map<String, Object> readMetadata(NotificationEntity notification) {
        if (notification.getMetadataJson() == null || notification.getMetadataJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(notification.getMetadataJson(), MAP_TYPE);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private OffsetDateTime parseTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
