package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.domain.repository.DeliveryAttemptRepository;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationDeliveryAttemptEntity;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryAttemptRepositoryImpl implements DeliveryAttemptRepository {

    private final SpringDataDeliveryAttemptJpaRepository repository;

    public DeliveryAttemptRepositoryImpl(SpringDataDeliveryAttemptJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationDeliveryAttemptEntity save(NotificationDeliveryAttemptEntity attempt) {
        return repository.save(attempt);
    }
}
