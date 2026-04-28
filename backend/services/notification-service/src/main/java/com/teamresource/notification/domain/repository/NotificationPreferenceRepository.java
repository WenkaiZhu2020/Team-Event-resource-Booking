package com.teamresource.notification.domain.repository;

import com.teamresource.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository {

    Optional<NotificationPreferenceEntity> findByUserId(UUID userId);

    NotificationPreferenceEntity save(NotificationPreferenceEntity preference);
}
