package com.teamresource.booking.domain.repository;

import com.teamresource.booking.infrastructure.persistence.entity.BookingIdempotencyKeyEntity;
import java.util.Optional;

public interface IdempotencyRepository {

    Optional<BookingIdempotencyKeyEntity> findByKey(String key);

    BookingIdempotencyKeyEntity save(BookingIdempotencyKeyEntity entity);
}
