package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpringDataNotificationJpaRepository extends JpaRepository<NotificationEntity, UUID>, JpaSpecificationExecutor<NotificationEntity> {

    Optional<NotificationEntity> findByIdempotencyKey(String idempotencyKey);

    List<NotificationEntity> findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            NotificationStatus status,
            OffsetDateTime nextRetryAt,
            Pageable pageable
    );

    List<NotificationEntity> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            NotificationStatus status,
            OffsetDateTime scheduledAt,
            Pageable pageable
    );
}
