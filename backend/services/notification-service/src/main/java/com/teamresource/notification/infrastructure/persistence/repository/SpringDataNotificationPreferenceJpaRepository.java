package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataNotificationPreferenceJpaRepository extends JpaRepository<NotificationPreferenceEntity, UUID> {
}
