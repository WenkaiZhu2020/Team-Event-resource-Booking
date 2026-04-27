package com.teamresource.workflow.infra.persistence;

import com.teamresource.workflow.domain.ApprovalStatus;
import com.teamresource.workflow.domain.ApprovalTargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {

    Optional<ApprovalRequestEntity> findByTargetTypeAndTargetId(ApprovalTargetType targetType, UUID targetId);

    List<ApprovalRequestEntity> findByApproverIdAndStatusOrderByCreatedAtDesc(UUID approverId, ApprovalStatus status);

    List<ApprovalRequestEntity> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    List<ApprovalRequestEntity> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);
}
