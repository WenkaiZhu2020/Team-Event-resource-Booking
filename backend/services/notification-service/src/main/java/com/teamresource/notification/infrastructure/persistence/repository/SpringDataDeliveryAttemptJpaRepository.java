package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.infrastructure.persistence.entity.NotificationDeliveryAttemptEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDeliveryAttemptJpaRepository extends JpaRepository<NotificationDeliveryAttemptEntity, UUID> {
}
