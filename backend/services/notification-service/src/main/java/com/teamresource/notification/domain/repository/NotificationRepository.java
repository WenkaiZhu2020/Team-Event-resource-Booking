package com.teamresource.notification.domain.repository;

import com.teamresource.notification.domain.model.NotificationSearchCriteria;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository {

    NotificationEntity save(NotificationEntity notification);

    Optional<NotificationEntity> findById(UUID id);

    Optional<NotificationEntity> findByIdempotencyKey(String idempotencyKey);

    Page<NotificationEntity> search(NotificationSearchCriteria criteria, Pageable pageable);

    List<NotificationEntity> findRetryableFailed(NotificationStatus status, OffsetDateTime now, int limit);

    List<NotificationEntity> findScheduledDue(NotificationStatus status, OffsetDateTime now, int limit);
}
