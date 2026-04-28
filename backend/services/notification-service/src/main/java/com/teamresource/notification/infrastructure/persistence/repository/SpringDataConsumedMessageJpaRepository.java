package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.infrastructure.persistence.entity.ConsumedMessageEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConsumedMessageJpaRepository extends JpaRepository<ConsumedMessageEntity, UUID> {

    Optional<ConsumedMessageEntity> findBySourceAndMessageId(String source, String messageId);
}
