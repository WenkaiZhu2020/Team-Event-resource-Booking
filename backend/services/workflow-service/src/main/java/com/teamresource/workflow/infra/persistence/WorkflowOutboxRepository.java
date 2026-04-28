package com.teamresource.workflow.infra.persistence;

import com.teamresource.workflow.domain.OutboxStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowOutboxRepository extends JpaRepository<WorkflowOutboxEntity, UUID> {

    List<WorkflowOutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<WorkflowOutboxEntity> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
