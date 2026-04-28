package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.domain.model.OutboxStatus;
import com.teamresource.booking.domain.repository.OutboxRepository;
import com.teamresource.booking.infrastructure.persistence.entity.BookingOutboxEntity;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepositoryImpl implements OutboxRepository {

    private final SpringDataBookingOutboxJpaRepository repository;

    public OutboxRepositoryImpl(SpringDataBookingOutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public BookingOutboxEntity save(BookingOutboxEntity entity) {
        return repository.save(entity);
    }

    @Override
    public List<BookingOutboxEntity> findByStatus(OutboxStatus status, int limit) {
        return repository.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(0, limit));
    }

    @Override
    public List<BookingOutboxEntity> findRetryableFailed(int maxAttempts, int limit) {
        return repository.findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
                OutboxStatus.FAILED,
                maxAttempts,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public void markPublished(BookingOutboxEntity entity, OffsetDateTime publishedAt) {
        entity.setStatus(OutboxStatus.PUBLISHED);
        entity.setPublishedAt(publishedAt);
        repository.save(entity);
    }

    @Override
    public void markFailed(BookingOutboxEntity entity, String reason) {
        entity.setStatus(OutboxStatus.FAILED);
        entity.setAttempts(entity.getAttempts() + 1);
        entity.setLastError(reason);
        repository.save(entity);
    }
}
