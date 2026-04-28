package com.teamresource.workflow.service;

import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import com.teamresource.workflow.domain.ApprovalAction;
import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.domain.ApprovalScope;
import com.teamresource.workflow.domain.ApprovalStatus;
import com.teamresource.workflow.domain.ApprovalStepStatus;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryEntity;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryRepository;
import com.teamresource.workflow.infra.persistence.ApprovalRequestEntity;
import com.teamresource.workflow.infra.persistence.ApprovalRequestRepository;
import com.teamresource.workflow.infra.persistence.ApprovalStepEntity;
import com.teamresource.workflow.infra.persistence.ApprovalStepRepository;
import com.teamresource.workflow.service.assignment.ApproverResolverChain;
import com.teamresource.workflow.service.command.ApproveApprovalCommand;
import com.teamresource.workflow.service.command.CompleteApprovalCallbackCommand;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import com.teamresource.workflow.service.command.RejectApprovalCommand;
import com.teamresource.workflow.service.state.ApprovalStateMachine;
import com.teamresource.workflow.service.template.ApprovalTemplateDefinition;
import com.teamresource.workflow.service.template.ApprovalTemplateResolverChain;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalTemplateResolverChain approvalTemplateResolverChain;
    private final ApproverResolverChain approverResolverChain;
    private final ApprovalStateMachine approvalStateMachine;
    private final ApprovalDecisionCallbackService approvalDecisionCallbackService;
    private final ApprovalOutboxService approvalOutboxService;
    private final ApprovalViewMapper approvalViewMapper;

    public ApprovalService(
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalDecisionHistoryRepository historyRepository,
            ApprovalStepRepository approvalStepRepository,
            ApprovalTemplateResolverChain approvalTemplateResolverChain,
            ApproverResolverChain approverResolverChain,
            ApprovalStateMachine approvalStateMachine,
            ApprovalDecisionCallbackService approvalDecisionCallbackService,
            ApprovalOutboxService approvalOutboxService,
            ApprovalViewMapper approvalViewMapper
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.historyRepository = historyRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.approvalTemplateResolverChain = approvalTemplateResolverChain;
        this.approverResolverChain = approverResolverChain;
        this.approvalStateMachine = approvalStateMachine;
        this.approvalDecisionCallbackService = approvalDecisionCallbackService;
        this.approvalOutboxService = approvalOutboxService;
        this.approvalViewMapper = approvalViewMapper;
    }

    @Transactional
    public ApprovalResponse create(CreateApprovalCommand command) {
        return approvalRequestRepository.findByTargetTypeAndTargetId(command.targetType(), command.targetId())
                .map(this::toResponse)
                .orElseGet(() -> createNewApproval(command));
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> pending(UUID currentUserId, boolean admin) {
        List<ApprovalRequestEntity> entities = admin
                ? approvalRequestRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING)
                : approvalRequestRepository.findByApproverIdAndStatusOrderByCreatedAtDesc(currentUserId, ApprovalStatus.PENDING);
        if (!admin) {
            entities = entities.stream()
                    .filter(entity -> entity.getApprovalScope() != ApprovalScope.ADMIN_ONLY)
                    .toList();
        }
        return entities.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> requested(UUID requesterId) {
        return approvalRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApprovalResponse byId(UUID approvalId, UUID currentUserId, boolean admin) {
        ApprovalRequestEntity entity = findApproval(approvalId);
        boolean accessibleToApprover = entity.getApprovalScope() != ApprovalScope.ADMIN_ONLY && entity.getApproverId().equals(currentUserId);
        if (!admin && !entity.getRequesterId().equals(currentUserId) && !accessibleToApprover) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approval access denied");
        }
        return toResponse(entity);
    }

    @Transactional
    public ApprovalResponse applyDecision(
            UUID approvalId,
            UUID actorId,
            boolean admin,
            String note,
            ApprovalDecisionType decisionType
    ) {
        ApprovalRequestEntity entity = findApproval(approvalId);
        requireApprover(entity, actorId, admin);
        ApprovalRequestEntity saved = applyDecision(entity, decisionType, trimToNull(note), actorId);
        return toResponse(saved);
    }

    private ApprovalResponse createNewApproval(CreateApprovalCommand command) {
        ApprovalTemplateDefinition template = approvalTemplateResolverChain.resolve(command);
        List<UUID> approverIds = resolveApproverIds(command);
        UUID approverId = approverIds.getFirst();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setApprovalId(UUID.randomUUID());
        entity.setTargetType(command.targetType());
        entity.setTargetId(command.targetId());
        entity.setApprovalType(template.approvalType());
        entity.setRequesterId(command.requesterId());
        entity.setApproverId(approverId);
        entity.setTargetOwnerId(command.targetOwnerId());
        entity.setResourceId(command.resourceId());
        entity.setTitle(template.title());
        entity.setSummary(template.summary());
        entity.setApprovalScope(template.approvalScope());
        entity.setCurrentStep(1);
        entity.setTotalSteps(Math.max(template.totalSteps(), approverIds.size()));
        entity.setStatus(ApprovalStatus.PENDING);
        entity.setSubmittedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        try {
            ApprovalRequestEntity saved = approvalRequestRepository.save(entity);
            createSteps(saved, approverIds, now);
            recordHistory(saved.getApprovalId(), ApprovalAction.CREATED, command.requesterId(), saved.getSummary(), now);
            approvalOutboxService.record("workflow.approval.created", saved);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            return approvalRequestRepository.findByTargetTypeAndTargetId(command.targetType(), command.targetId())
                    .map(this::toResponse)
                    .orElseThrow(() -> ex);
        }
    }

    private ApprovalRequestEntity applyDecision(
            ApprovalRequestEntity entity,
            ApprovalDecisionType decisionType,
            String note,
            UUID actorId
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (entity.getStatus() != ApprovalStatus.PENDING) {
            approvalStateMachine.transition(entity.getStatus(), decisionType);
        }
        ApprovalStepEntity currentStep = approvalStepRepository.findByApprovalIdAndStepNumber(entity.getApprovalId(), entity.getCurrentStep())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Approval step not found"));
        updateCurrentStep(currentStep, decisionType, note, now);

        ApprovalAction action = decisionType == ApprovalDecisionType.APPROVE ? ApprovalAction.APPROVED : ApprovalAction.REJECTED;
        ApprovalRequestEntity saved;

        if (decisionType == ApprovalDecisionType.APPROVE && entity.getCurrentStep() < entity.getTotalSteps()) {
            ApprovalStepEntity nextStep = approvalStepRepository.findByApprovalIdAndStepNumber(entity.getApprovalId(), entity.getCurrentStep() + 1)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Next approval step not found"));
            nextStep.setStatus(ApprovalStepStatus.PENDING);
            nextStep.setUpdatedAt(now);
            approvalStepRepository.save(nextStep);

            entity.setCurrentStep(entity.getCurrentStep() + 1);
            entity.setApproverId(nextStep.getApproverId());
            entity.setUpdatedAt(now);
            saved = approvalRequestRepository.save(entity);
            recordHistory(saved.getApprovalId(), action, actorId, note, now);
            approvalOutboxService.record("workflow.approval.step-approved", saved);
            return saved;
        }

        ApprovalStatus nextStatus = approvalStateMachine.transition(entity.getStatus(), decisionType);
        entity.setStatus(nextStatus);
        entity.setDecidedAt(now);
        entity.setDecisionNote(note);
        entity.setCurrentStep(entity.getTotalSteps());
        entity.setUpdatedAt(now);
        saved = approvalRequestRepository.save(entity);

        if (decisionType == ApprovalDecisionType.REJECT) {
            cancelRemainingSteps(saved, now);
        }
        recordHistory(saved.getApprovalId(), action, actorId, note, now);
        approvalOutboxService.record(decisionType == ApprovalDecisionType.APPROVE
                ? "workflow.approval.approved"
                : "workflow.approval.rejected", saved);
        approvalDecisionCallbackService.complete(new CompleteApprovalCallbackCommand(
                saved.getApprovalId(),
                saved.getTargetType(),
                saved.getTargetId(),
                saved.getStatus().name(),
                note
        ));
        return saved;
    }

    private List<UUID> resolveApproverIds(CreateApprovalCommand command) {
        LinkedHashSet<UUID> approverIds = new LinkedHashSet<>();
        approverIds.add(approverResolverChain.resolve(toCreateApprovalRequest(command)));
        if (command.additionalApproverIds() != null) {
            approverIds.addAll(command.additionalApproverIds().stream().filter(java.util.Objects::nonNull).toList());
        }
        return new ArrayList<>(approverIds);
    }

    private void createSteps(ApprovalRequestEntity entity, List<UUID> approverIds, OffsetDateTime now) {
        for (int i = 0; i < approverIds.size(); i++) {
            ApprovalStepEntity step = new ApprovalStepEntity();
            step.setStepId(UUID.randomUUID());
            step.setApprovalId(entity.getApprovalId());
            step.setStepNumber(i + 1);
            step.setApproverId(approverIds.get(i));
            step.setStatus(i == 0 ? ApprovalStepStatus.PENDING : ApprovalStepStatus.WAITING);
            step.setCreatedAt(now);
            step.setUpdatedAt(now);
            approvalStepRepository.save(step);
        }
    }

    private void updateCurrentStep(ApprovalStepEntity step, ApprovalDecisionType decisionType, String note, OffsetDateTime now) {
        step.setStatus(decisionType == ApprovalDecisionType.APPROVE ? ApprovalStepStatus.APPROVED : ApprovalStepStatus.REJECTED);
        step.setDecisionNote(note);
        step.setDecidedAt(now);
        step.setUpdatedAt(now);
        approvalStepRepository.save(step);
    }

    private void cancelRemainingSteps(ApprovalRequestEntity entity, OffsetDateTime now) {
        approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(entity.getApprovalId()).stream()
                .filter(step -> step.getStepNumber() > entity.getCurrentStep())
                .filter(step -> step.getStatus() == ApprovalStepStatus.WAITING || step.getStatus() == ApprovalStepStatus.PENDING)
                .forEach(step -> {
                    step.setStatus(ApprovalStepStatus.CANCELLED);
                    step.setUpdatedAt(now);
                    approvalStepRepository.save(step);
                });
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
        if (entity.getApprovalScope() == ApprovalScope.ADMIN_ONLY) {
            if (admin) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This approval requires an administrator decision");
        }
        if (admin || entity.getApproverId().equals(actorId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approval decision access denied");
    }

    private ApprovalRequestEntity findApproval(UUID approvalId) {
        return approvalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));
    }

    private ApprovalResponse toResponse(ApprovalRequestEntity entity) {
        return approvalViewMapper.toResponse(
                entity,
                approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(entity.getApprovalId()),
                historyRepository.findByApprovalIdOrderByActedAtAsc(entity.getApprovalId())
        );
    }

    private CreateApprovalRequest toCreateApprovalRequest(CreateApprovalCommand command) {
        return new CreateApprovalRequest(
                command.targetType(),
                command.targetId(),
                command.requesterId(),
                command.approverId(),
                command.targetOwnerId(),
                command.resourceId(),
                command.title(),
                command.approvalType(),
                command.summary(),
                command.startAt(),
                command.endAt(),
                command.additionalApproverIds()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
