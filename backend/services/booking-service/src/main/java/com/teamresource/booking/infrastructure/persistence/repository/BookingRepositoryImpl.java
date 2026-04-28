package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.application.specification.BookingSpecifications;
import com.teamresource.booking.domain.model.BookingSearchCriteria;
import com.teamresource.booking.domain.model.BookingStatus;
import com.teamresource.booking.domain.repository.BookingRepository;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class BookingRepositoryImpl implements BookingRepository {

    private final SpringDataBookingJpaRepository repository;

    public BookingRepositoryImpl(SpringDataBookingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public BookingEntity save(BookingEntity entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<BookingEntity> findById(UUID bookingId) {
        return repository.findById(bookingId);
    }

    @Override
    public boolean existsOverlappingActiveBooking(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt, List<BookingStatus> statuses) {
        return repository.existsOverlap(resourceId, startAt, endAt, statuses);
    }

    @Override
    public Page<BookingEntity> search(BookingSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(BookingSpecifications.fromCriteria(criteria), pageable);
    }

    @Override
    public List<BookingEntity> findFutureByEvent(UUID eventId, OffsetDateTime now) {
        return repository.findFutureByEvent(eventId, now);
    }

    @Override
    public List<BookingEntity> findFutureByResource(UUID resourceId, OffsetDateTime now) {
        return repository.findFutureByResource(resourceId, now);
    }
}
