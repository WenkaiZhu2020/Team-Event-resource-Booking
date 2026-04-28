package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.domain.repository.NotificationPreferenceRepository;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationPreferenceRepositoryImpl implements NotificationPreferenceRepository {

    private final SpringDataNotificationPreferenceJpaRepository repository;

    public NotificationPreferenceRepositoryImpl(SpringDataNotificationPreferenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<NotificationPreferenceEntity> findByUserId(UUID userId) {
        return repository.findById(userId);
    }

    @Override
    public NotificationPreferenceEntity save(NotificationPreferenceEntity preference) {
        return repository.save(preference);
    }
}
