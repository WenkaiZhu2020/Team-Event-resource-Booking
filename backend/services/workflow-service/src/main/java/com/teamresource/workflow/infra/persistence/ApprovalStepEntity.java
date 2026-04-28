package com.teamresource.workflow.infra.persistence;

import com.teamresource.workflow.domain.ApprovalStepStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "approval_steps",
        schema = "workflows",
        uniqueConstraints = @UniqueConstraint(name = "uk_approval_step_number", columnNames = {"approval_id", "step_number"})
)
public class ApprovalStepEntity {

    @Id
    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(name = "approval_id", nullable = false)
    private UUID approvalId;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalStepStatus status;

    @Column(name = "decision_note", length = 400)
    private String decisionNote;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getStepId() {
        return stepId;
    }

    public void setStepId(UUID stepId) {
        this.stepId = stepId;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(UUID approvalId) {
        this.approvalId = approvalId;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public ApprovalStepStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStepStatus status) {
        this.status = status;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(OffsetDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
