package com.teamresource.workflow.infra.persistence;

import com.teamresource.workflow.domain.ApprovalAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "approval_decision_history", schema = "workflows")
public class ApprovalDecisionHistoryEntity {

    @Id
    @Column(name = "decision_id", nullable = false)
    private UUID decisionId;

    @Column(name = "approval_id", nullable = false)
    private UUID approvalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalAction action;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(length = 400)
    private String note;

    @Column(name = "acted_at", nullable = false)
    private OffsetDateTime actedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(UUID decisionId) {
        this.decisionId = decisionId;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(UUID approvalId) {
        this.approvalId = approvalId;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public void setAction(ApprovalAction action) {
        this.action = action;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OffsetDateTime getActedAt() {
        return actedAt;
    }

    public void setActedAt(OffsetDateTime actedAt) {
        this.actedAt = actedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
