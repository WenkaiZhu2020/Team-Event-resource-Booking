package com.teamresource.booking.domain.repository;

import com.teamresource.booking.infrastructure.persistence.entity.ConsumedMessageEntity;

public interface ConsumedMessageRepository {

    boolean exists(String source, String messageId);

    ConsumedMessageEntity save(ConsumedMessageEntity entity);
}
