package com.teamresource.booking.domain.repository;

import com.teamresource.booking.domain.model.BookingSearchCriteria;
import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingRepository {

    BookingEntity save(BookingEntity entity);

    Optional<BookingEntity> findById(UUID bookingId);

    boolean existsOverlappingActiveBooking(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt, List<BookingStatus> statuses);

    Page<BookingEntity> search(BookingSearchCriteria criteria, Pageable pageable);

    List<BookingEntity> findFutureByEvent(UUID eventId, OffsetDateTime now);

    List<BookingEntity> findFutureByResource(UUID resourceId, OffsetDateTime now);
}
