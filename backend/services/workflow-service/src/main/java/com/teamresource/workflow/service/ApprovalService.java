package com.teamresource.workflow.service;

import com.teamresource.workflow.api.dto.ApprovalDecisionCallbackRequest;
import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import com.teamresource.workflow.domain.ApprovalAction;
import com.teamresource.workflow.domain.ApprovalStatus;
import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.infra.client.BookingWorkflowClient;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryEntity;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryRepository;
import com.teamresource.workflow.infra.persistence.ApprovalRequestEntity;
import com.teamresource.workflow.infra.persistence.ApprovalRequestRepository;
import com.teamresource.workflow.service.assignment.ApproverResolverChain;
import com.teamresource.workflow.service.template.BookingApprovalWorkflowTemplate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalDecisionHistoryRepository historyRepository;
    private final BookingApprovalWorkflowTemplate bookingApprovalWorkflowTemplate;
    private final ApproverResolverChain approverResolverChain;
    private final BookingWorkflowClient bookingWorkflowClient;

    public ApprovalService(
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalDecisionHistoryRepository historyRepository,
            BookingApprovalWorkflowTemplate bookingApprovalWorkflowTemplate,
            ApproverResolverChain approverResolverChain,
            BookingWorkflowClient bookingWorkflowClient
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.historyRepository = historyRepository;
        this.bookingApprovalWorkflowTemplate = bookingApprovalWorkflowTemplate;
        this.approverResolverChain = approverResolverChain;
        this.bookingWorkflowClient = bookingWorkflowClient;
    }

    @Transactional
    public ApprovalResponse create(CreateApprovalRequest request) {
        return approvalRequestRepository.findByTargetTypeAndTargetId(request.targetType(), request.targetId())
                .map(this::toResponse)
                .orElseGet(() -> createNewApproval(request));
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> pending(UUID currentUserId, boolean admin) {
        List<ApprovalRequestEntity> entities = admin
                ? approvalRequestRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING)
                : approvalRequestRepository.findByApproverIdAndStatusOrderByCreatedAtDesc(currentUserId, ApprovalStatus.PENDING);
        return entities.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> requested(UUID requesterId) {
        return approvalRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApprovalResponse byId(UUID approvalId, UUID currentUserId, boolean admin) {
        ApprovalRequestEntity entity = findApproval(approvalId);
        if (!admin && !entity.getRequesterId().equals(currentUserId) && !entity.getApproverId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approval access denied");
        }
        return toResponse(entity);
    }

    @Transactional
    public ApprovalResponse approve(ApprovalDecisionCommand command) {
        ApprovalRequestEntity entity = findApproval(command.approvalId());
        requireApprover(entity, command.actorId(), command.admin());
        requirePending(entity);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setStatus(ApprovalStatus.APPROVED);
        entity.setDecidedAt(now);
        entity.setDecisionNote(trimToNull(command.note()));
        entity.setUpdatedAt(now);
        ApprovalRequestEntity saved = approvalRequestRepository.save(entity);
        recordHistory(saved.getApprovalId(), ApprovalAction.APPROVED, command.actorId(), command.note(), now);
        applyTargetDecision(saved, "APPROVED", command.note());
        return toResponse(saved);
    }

    @Transactional
    public ApprovalResponse reject(ApprovalDecisionCommand command) {
        ApprovalRequestEntity entity = findApproval(command.approvalId());
        requireApprover(entity, command.actorId(), command.admin());
        requirePending(entity);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setStatus(ApprovalStatus.REJECTED);
        entity.setDecidedAt(now);
        entity.setDecisionNote(trimToNull(command.note()));
        entity.setUpdatedAt(now);
        ApprovalRequestEntity saved = approvalRequestRepository.save(entity);
        recordHistory(saved.getApprovalId(), ApprovalAction.REJECTED, command.actorId(), command.note(), now);
        applyTargetDecision(saved, "REJECTED", command.note());
        return toResponse(saved);
    }

    private ApprovalResponse createNewApproval(CreateApprovalRequest request) {
        if (request.targetType() != ApprovalTargetType.BOOKING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This version only supports booking approval workflows");
        }
        ApprovalRequestEntity entity = bookingApprovalWorkflowTemplate.build(request, approverResolverChain);
        try {
            ApprovalRequestEntity saved = approvalRequestRepository.save(entity);
            recordHistory(saved.getApprovalId(), ApprovalAction.CREATED, request.requesterId(), request.summary(), saved.getCreatedAt());
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            return approvalRequestRepository.findByTargetTypeAndTargetId(request.targetType(), request.targetId())
                    .map(this::toResponse)
                    .orElseThrow(() -> ex);
        }
    }

    private void applyTargetDecision(ApprovalRequestEntity entity, String decision, String note) {
        if (entity.getTargetType() == ApprovalTargetType.BOOKING) {
            bookingWorkflowClient.applyDecision(entity.getTargetId(), new ApprovalDecisionCallbackRequest(decision, note));
        }
    }

    private void recordHistory(UUID approvalId, ApprovalAction action, UUID actorId, String note, OffsetDateTime actedAt) {
        ApprovalDecisionHistoryEntity history = new ApprovalDecisionHistoryEntity();
        history.setDecisionId(UUID.randomUUID());
        history.setApprovalId(approvalId);
        history.setAction(action);
        history.setActorId(actorId);
        history.setNote(trimToNull(note));
        history.setActedAt(actedAt);
        history.setCreatedAt(actedAt);
        historyRepository.save(history);
    }

    private void requireApprover(ApprovalRequestEntity entity, UUID actorId, boolean admin) {
        if (admin || entity.getApproverId().equals(actorId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approval decision access denied");
    }

    private void requirePending(ApprovalRequestEntity entity) {
        if (entity.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending approvals can be decided");
        }
    }

    private ApprovalRequestEntity findApproval(UUID approvalId) {
        return approvalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));
    }

    private ApprovalResponse toResponse(ApprovalRequestEntity entity) {
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
                entity.getStatus().name(),
                entity.getSubmittedAt(),
                entity.getDecidedAt(),
                entity.getDecisionNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                historyRepository.findByApprovalIdOrderByActedAtAsc(entity.getApprovalId()).stream()
                        .map(history -> new com.teamresource.workflow.api.dto.ApprovalHistoryResponse(
                                history.getDecisionId(),
                                history.getAction().name(),
                                history.getActorId(),
                                history.getNote(),
                                history.getActedAt(),
                                history.getCreatedAt()
                        ))
                        .toList()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
