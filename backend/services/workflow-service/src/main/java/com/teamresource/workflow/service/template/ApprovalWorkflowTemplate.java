package com.teamresource.workflow.service.template;

import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import com.teamresource.workflow.domain.ApprovalStatus;
import com.teamresource.workflow.infra.persistence.ApprovalRequestEntity;
import com.teamresource.workflow.service.assignment.ApproverResolverChain;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public abstract class ApprovalWorkflowTemplate {

    public ApprovalRequestEntity build(CreateApprovalRequest request, ApproverResolverChain approverResolverChain) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setApprovalId(UUID.randomUUID());
        entity.setTargetType(request.targetType());
        entity.setTargetId(request.targetId());
        entity.setApprovalType(resolveApprovalType(request));
        entity.setRequesterId(request.requesterId());
        entity.setApproverId(approverResolverChain.resolve(request));
        entity.setTargetOwnerId(request.targetOwnerId());
        entity.setResourceId(request.resourceId());
        entity.setTitle(resolveTitle(request));
        entity.setSummary(resolveSummary(request));
        entity.setCurrentStep(1);
        entity.setTotalSteps(1);
        entity.setStatus(ApprovalStatus.PENDING);
        entity.setSubmittedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    protected abstract String resolveApprovalType(CreateApprovalRequest request);

    protected abstract String resolveTitle(CreateApprovalRequest request);

    protected String resolveSummary(CreateApprovalRequest request) {
        return request.summary();
    }
}
