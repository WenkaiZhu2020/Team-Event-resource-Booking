package com.teamresource.workflow.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumedMessageRepository extends JpaRepository<ConsumedMessageEntity, UUID> {

    boolean existsBySourceAndMessageId(String source, String messageId);
}
