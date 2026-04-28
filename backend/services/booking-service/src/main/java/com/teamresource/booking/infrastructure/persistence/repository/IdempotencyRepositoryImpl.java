package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.domain.repository.IdempotencyRepository;
import com.teamresource.booking.infrastructure.persistence.entity.BookingIdempotencyKeyEntity;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRepositoryImpl implements IdempotencyRepository {

    private final SpringDataBookingIdempotencyJpaRepository repository;

    public IdempotencyRepositoryImpl(SpringDataBookingIdempotencyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<BookingIdempotencyKeyEntity> findByKey(String key) {
        return repository.findByIdempotencyKey(key);
    }

    @Override
    public BookingIdempotencyKeyEntity save(BookingIdempotencyKeyEntity entity) {
        return repository.saveAndFlush(entity);
    }
}
