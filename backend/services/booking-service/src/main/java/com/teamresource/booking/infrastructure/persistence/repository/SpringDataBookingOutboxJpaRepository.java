package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.domain.model.OutboxStatus;
import com.teamresource.booking.infrastructure.persistence.entity.BookingOutboxEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBookingOutboxJpaRepository extends JpaRepository<BookingOutboxEntity, UUID> {

    List<BookingOutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    List<BookingOutboxEntity> findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
            OutboxStatus status,
            Integer attempts,
            Pageable pageable
    );
}
