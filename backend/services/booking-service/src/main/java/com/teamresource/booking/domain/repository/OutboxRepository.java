package com.teamresource.booking.domain.repository;

import com.teamresource.booking.domain.model.OutboxStatus;
import com.teamresource.booking.infrastructure.persistence.entity.BookingOutboxEntity;
import java.time.OffsetDateTime;
import java.util.List;

public interface OutboxRepository {

    BookingOutboxEntity save(BookingOutboxEntity entity);

    List<BookingOutboxEntity> findByStatus(OutboxStatus status, int limit);

    List<BookingOutboxEntity> findRetryableFailed(int maxAttempts, int limit);

    void markPublished(BookingOutboxEntity entity, OffsetDateTime publishedAt);

    void markFailed(BookingOutboxEntity entity, String reason);
}
