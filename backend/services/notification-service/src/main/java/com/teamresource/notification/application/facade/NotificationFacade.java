package com.teamresource.notification.application.facade;

import com.teamresource.notification.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.notification.application.pipeline.NotificationDispatchCommand;
import com.teamresource.notification.application.service.NotificationDispatchService;
import com.teamresource.notification.application.service.NotificationPreferenceService;
import com.teamresource.notification.application.service.NotificationQueryService;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.model.NotificationType;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class NotificationFacade {

    private final NotificationDispatchService dispatchService;
    private final NotificationQueryService queryService;
    private final NotificationPreferenceService preferenceService;

    public NotificationFacade(
            NotificationDispatchService dispatchService,
            NotificationQueryService queryService,
            NotificationPreferenceService preferenceService
    ) {
        this.dispatchService = dispatchService;
        this.queryService = queryService;
        this.preferenceService = preferenceService;
    }

    public List<NotificationEntity> dispatch(NotificationDispatchCommand command) {
        return dispatchService.dispatch(command);
    }

    public int retryFailed(int limit) {
        return dispatchService.retryFailed(limit);
    }

    public int sendDueReminders(int limit) {
        return dispatchService.sendDueReminders(limit);
    }

    public Page<NotificationEntity> listForUser(
            UUID userId,
            NotificationStatus status,
            NotificationChannel channel,
            NotificationType type,
            Boolean unreadOnly,
            Pageable pageable
    ) {
        return queryService.listForUser(userId, status, channel, type, unreadOnly, pageable);
    }

    public NotificationEntity getById(UUID userId, UUID notificationId, boolean admin) {
        return queryService.getById(userId, notificationId, admin);
    }

    public NotificationEntity markRead(UUID userId, UUID notificationId, boolean admin) {
        return queryService.markRead(userId, notificationId, admin);
    }

    public int markReadBatch(UUID userId, List<UUID> notificationIds, boolean admin) {
        return queryService.markReadBatch(userId, notificationIds, admin);
    }

    public NotificationPreferenceEntity getPreference(UUID userId) {
        return preferenceService.getOrCreate(userId);
    }

    public NotificationPreferenceEntity updatePreference(UUID userId, UpdateNotificationPreferenceRequest request) {
        return preferenceService.update(userId, request);
    }
}
