package com.teamresource.notification.domain.repository;

import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import java.util.Optional;

public interface NotificationTemplateRepository {

    Optional<NotificationTemplateEntity> findActive(String templateCode, NotificationChannel channel);
}
