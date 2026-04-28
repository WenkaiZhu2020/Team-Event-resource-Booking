package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.domain.repository.ConsumedMessageRepository;
import com.teamresource.booking.infrastructure.persistence.entity.ConsumedMessageEntity;
import org.springframework.stereotype.Repository;

@Repository
public class ConsumedMessageRepositoryImpl implements ConsumedMessageRepository {

    private final SpringDataConsumedMessageJpaRepository repository;

    public ConsumedMessageRepositoryImpl(SpringDataConsumedMessageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(String source, String messageId) {
        return repository.existsBySourceAndMessageId(source, messageId);
    }

    @Override
    public ConsumedMessageEntity save(ConsumedMessageEntity entity) {
        return repository.save(entity);
    }
}
