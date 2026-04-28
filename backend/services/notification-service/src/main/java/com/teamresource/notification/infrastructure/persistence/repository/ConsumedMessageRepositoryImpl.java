package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.domain.repository.ConsumedMessageRepository;
import com.teamresource.notification.infrastructure.persistence.entity.ConsumedMessageEntity;
import org.springframework.stereotype.Repository;

@Repository
public class ConsumedMessageRepositoryImpl implements ConsumedMessageRepository {

    private final SpringDataConsumedMessageJpaRepository repository;

    public ConsumedMessageRepositoryImpl(SpringDataConsumedMessageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(String source, String messageId) {
        return repository.findBySourceAndMessageId(source, messageId).isPresent();
    }

    @Override
    public ConsumedMessageEntity save(ConsumedMessageEntity entity) {
        return repository.save(entity);
    }
}
