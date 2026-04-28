package com.teamresource.notification.domain.repository;

import com.teamresource.notification.infrastructure.persistence.entity.ConsumedMessageEntity;

public interface ConsumedMessageRepository {

    boolean exists(String source, String messageId);

    ConsumedMessageEntity save(ConsumedMessageEntity entity);
}
