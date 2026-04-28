package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.infrastructure.persistence.entity.ConsumedMessageEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConsumedMessageJpaRepository extends JpaRepository<ConsumedMessageEntity, UUID> {

    boolean existsBySourceAndMessageId(String source, String messageId);
}
