package com.teamresource.notification.application.service;

import com.teamresource.notification.api.dto.NotificationPreferenceResponse;
import com.teamresource.notification.api.dto.NotificationResponse;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationViewMapper {

    public NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getChannel(),
                entity.getStatus(),
                entity.getTitle(),
                entity.getBody(),
                entity.getSource(),
                entity.getSourceEventType(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getScheduledAt(),
                entity.getSentAt(),
                entity.getReadAt(),
                entity.getCreatedAt()
        );
    }

    public NotificationPreferenceResponse toPreferenceResponse(NotificationPreferenceEntity entity) {
        return new NotificationPreferenceResponse(
                entity.getUserId(),
                entity.isInAppEnabled(),
                entity.isEmailEnabled(),
                entity.getReminderLeadMinutes()
        );
    }
}
