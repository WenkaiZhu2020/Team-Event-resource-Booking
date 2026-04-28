package com.teamresource.workflow.service;

import com.teamresource.workflow.api.dto.ApprovalHistoryResponse;
import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.api.dto.ApprovalStepResponse;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryEntity;
import com.teamresource.workflow.infra.persistence.ApprovalRequestEntity;
import com.teamresource.workflow.infra.persistence.ApprovalStepEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApprovalViewMapper {

    public ApprovalResponse toResponse(
            ApprovalRequestEntity entity,
            List<ApprovalStepEntity> steps,
            List<ApprovalDecisionHistoryEntity> history
    ) {
        return new ApprovalResponse(
                entity.getApprovalId(),
                entity.getTargetType().name(),
                entity.getTargetId(),
                entity.getApprovalType(),
                entity.getRequesterId(),
                entity.getApproverId(),
                entity.getTargetOwnerId(),
                entity.getResourceId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getCurrentStep(),
                entity.getTotalSteps(),
                entity.getApprovalScope().name(),
                entity.getStatus().name(),
                entity.getSubmittedAt(),
                entity.getDecidedAt(),
                entity.getDecisionNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                steps.stream().map(this::toStep).toList(),
                history.stream().map(this::toHistory).toList()
        );
    }

    private ApprovalStepResponse toStep(ApprovalStepEntity step) {
        return new ApprovalStepResponse(
                step.getStepId(),
                step.getStepNumber(),
                step.getApproverId(),
                step.getStatus().name(),
                step.getDecisionNote(),
                step.getDecidedAt(),
                step.getCreatedAt(),
                step.getUpdatedAt()
        );
    }

    private ApprovalHistoryResponse toHistory(ApprovalDecisionHistoryEntity history) {
        return new ApprovalHistoryResponse(
                history.getDecisionId(),
                history.getAction().name(),
                history.getActorId(),
                history.getNote(),
                history.getActedAt(),
                history.getCreatedAt()
        );
    }
}
