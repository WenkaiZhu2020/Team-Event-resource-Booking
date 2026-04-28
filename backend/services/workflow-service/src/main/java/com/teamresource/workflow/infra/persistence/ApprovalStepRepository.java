package com.teamresource.workflow.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStepEntity, UUID> {

    List<ApprovalStepEntity> findByApprovalIdOrderByStepNumberAsc(UUID approvalId);

    Optional<ApprovalStepEntity> findByApprovalIdAndStepNumber(UUID approvalId, int stepNumber);
}
