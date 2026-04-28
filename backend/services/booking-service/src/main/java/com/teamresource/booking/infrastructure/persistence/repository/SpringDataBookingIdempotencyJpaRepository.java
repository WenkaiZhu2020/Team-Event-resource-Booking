package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.infrastructure.persistence.entity.BookingIdempotencyKeyEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBookingIdempotencyJpaRepository extends JpaRepository<BookingIdempotencyKeyEntity, UUID> {

    Optional<BookingIdempotencyKeyEntity> findByIdempotencyKey(String idempotencyKey);
}
