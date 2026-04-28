package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.application.service.NotificationSpecifications;
import com.teamresource.notification.domain.model.NotificationSearchCriteria;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.repository.NotificationRepository;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final SpringDataNotificationJpaRepository repository;

    public NotificationRepositoryImpl(SpringDataNotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationEntity save(NotificationEntity notification) {
        return repository.save(notification);
    }

    @Override
    public Optional<NotificationEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<NotificationEntity> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Page<NotificationEntity> search(NotificationSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(NotificationSpecifications.from(criteria), pageable);
    }

    @Override
    public List<NotificationEntity> findRetryableFailed(NotificationStatus status, OffsetDateTime now, int limit) {
        return repository.findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        status,
                        now,
                        PageRequest.of(0, Math.max(1, limit * 2)))
                .stream()
                .filter(n -> n.getRetryCount() < n.getMaxRetries())
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    public List<NotificationEntity> findScheduledDue(NotificationStatus status, OffsetDateTime now, int limit) {
        return repository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                status,
                now,
                PageRequest.of(0, Math.max(1, limit))
        );
    }
}
