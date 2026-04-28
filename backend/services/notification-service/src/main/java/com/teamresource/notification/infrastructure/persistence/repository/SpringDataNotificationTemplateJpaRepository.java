package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataNotificationTemplateJpaRepository extends JpaRepository<NotificationTemplateEntity, UUID> {

    Optional<NotificationTemplateEntity> findByTemplateCodeAndChannelAndActiveTrue(String templateCode, NotificationChannel channel);
}
