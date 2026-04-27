package com.teamresource.workflow.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDecisionHistoryRepository extends JpaRepository<ApprovalDecisionHistoryEntity, UUID> {

    List<ApprovalDecisionHistoryEntity> findByApprovalIdOrderByActedAtAsc(UUID approvalId);
}
