package com.teamresource.notification.domain.repository;

import com.teamresource.notification.infrastructure.persistence.entity.NotificationDeliveryAttemptEntity;

public interface DeliveryAttemptRepository {

    NotificationDeliveryAttemptEntity save(NotificationDeliveryAttemptEntity attempt);
}
