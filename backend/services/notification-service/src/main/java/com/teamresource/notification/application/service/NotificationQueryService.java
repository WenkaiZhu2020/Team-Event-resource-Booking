package com.teamresource.notification.application.service;

import com.teamresource.notification.common.error.ApiException;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationSearchCriteria;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.model.NotificationType;
import com.teamresource.notification.domain.repository.NotificationRepository;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> listForUser(
            UUID userId,
            NotificationStatus status,
            NotificationChannel channel,
            NotificationType type,
            Boolean unreadOnly,
            Pageable pageable
    ) {
        NotificationSearchCriteria criteria = new NotificationSearchCriteria(userId, status, channel, type, unreadOnly);
        return notificationRepository.search(criteria, pageable);
    }

    @Transactional(readOnly = true)
    public NotificationEntity getById(UUID userId, UUID notificationId, boolean admin) {
        NotificationEntity entity = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException("NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "Notification not found"));

        if (!admin && !userId.equals(entity.getUserId())) {
            throw new ApiException("NOTIFICATION_FORBIDDEN", HttpStatus.FORBIDDEN, "Notification does not belong to user");
        }

        return entity;
    }

    @Transactional
    public NotificationEntity markRead(UUID userId, UUID notificationId, boolean admin) {
        NotificationEntity entity = getById(userId, notificationId, admin);
        if (entity.getReadAt() != null) {
            return entity;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setReadAt(now);
        entity.setStatus(NotificationStatus.READ);
        entity.setUpdatedAt(now);
        return notificationRepository.save(entity);
    }

    @Transactional
    public int markReadBatch(UUID userId, List<UUID> notificationIds, boolean admin) {
        int updated = 0;
        for (UUID id : notificationIds) {
            NotificationEntity entity = getById(userId, id, admin);
            if (entity.getReadAt() != null) {
                continue;
            }
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            entity.setReadAt(now);
            entity.setStatus(NotificationStatus.READ);
            entity.setUpdatedAt(now);
            notificationRepository.save(entity);
            updated++;
        }
        return updated;
    }
}
