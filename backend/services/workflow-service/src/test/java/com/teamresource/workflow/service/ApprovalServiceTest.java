package com.teamresource.workflow.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.teamresource.workflow.config.ClientProperties;
import com.teamresource.workflow.domain.ApprovalAction;
import com.teamresource.workflow.domain.ApprovalDecisionType;
import com.teamresource.workflow.domain.ApprovalScope;
import com.teamresource.workflow.domain.ApprovalStepStatus;
import com.teamresource.workflow.domain.ApprovalStatus;
import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.infra.client.BookingWorkflowClient;
import com.teamresource.workflow.infra.client.EventWorkflowClient;
import com.teamresource.workflow.infra.client.ResourceApprovalPolicyGateway;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryEntity;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryRepository;
import com.teamresource.workflow.infra.persistence.ApprovalRequestEntity;
import com.teamresource.workflow.infra.persistence.ApprovalRequestRepository;
import com.teamresource.workflow.infra.persistence.ApprovalStepEntity;
import com.teamresource.workflow.infra.persistence.ApprovalStepRepository;
import com.teamresource.workflow.infra.persistence.WorkflowOutboxEntity;
import com.teamresource.workflow.infra.persistence.WorkflowOutboxRepository;
import com.teamresource.workflow.service.assignment.ApproverResolverChain;
import com.teamresource.workflow.service.assignment.ExplicitApproverResolver;
import com.teamresource.workflow.service.assignment.TargetOwnerApproverResolver;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
import com.teamresource.workflow.service.state.ApprovalStateMachine;
import com.teamresource.workflow.service.state.ApprovedApprovalStateHandler;
import com.teamresource.workflow.service.state.CancelledApprovalStateHandler;
import com.teamresource.workflow.service.state.PendingApprovalStateHandler;
import com.teamresource.workflow.service.state.RejectedApprovalStateHandler;
import com.teamresource.workflow.service.template.ApprovalTemplateResolverChain;
import com.teamresource.workflow.service.template.BookingApprovalTemplateHandler;
import com.teamresource.workflow.service.template.EventApprovalTemplateHandler;
import com.teamresource.workflow.service.template.ExplicitRolesTemplateHandler;
import com.teamresource.workflow.service.template.FallbackTemplateHandler;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private ApprovalDecisionHistoryRepository historyRepository;

    @Mock
    private ApprovalStepRepository approvalStepRepository;

    @Mock
    private WorkflowOutboxRepository workflowOutboxRepository;

    @Mock
    private ResourceApprovalPolicyGateway resourceApprovalPolicyGateway;

    private StubBookingWorkflowClient bookingWorkflowClient;
    private StubEventWorkflowClient eventWorkflowClient;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        bookingWorkflowClient = new StubBookingWorkflowClient();
        eventWorkflowClient = new StubEventWorkflowClient();
        approvalService = new ApprovalService(
                approvalRequestRepository,
                historyRepository,
                approvalStepRepository,
                new ApprovalTemplateResolverChain(List.of(
                        new ExplicitRolesTemplateHandler(),
                        new EventApprovalTemplateHandler(),
                        new BookingApprovalTemplateHandler(resourceApprovalPolicyGateway),
                        new FallbackTemplateHandler()
                )),
                new ApproverResolverChain(List.of(new ExplicitApproverResolver(), new TargetOwnerApproverResolver())),
                new ApprovalStateMachine(List.of(
                        new PendingApprovalStateHandler(),
                        new ApprovedApprovalStateHandler(),
                        new RejectedApprovalStateHandler(),
                        new CancelledApprovalStateHandler()
                )),
                new ApprovalDecisionCallbackService(bookingWorkflowClient, eventWorkflowClient),
                new ApprovalOutboxService(workflowOutboxRepository, JsonMapper.builder().findAndAddModules().build()),
                new ApprovalViewMapper()
        );
    }

    @Test
    void createShouldPersistBookingApprovalStepHistoryAndOutbox() {
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(resourceApprovalPolicyGateway.resolve(resourceId))
                .thenReturn(new ResourceApprovalPolicyGateway.ResourceApprovalPolicy(resourceId, managerId, "MANAGER_APPROVAL", true));
        when(approvalRequestRepository.findByTargetTypeAndTargetId(ApprovalTargetType.BOOKING, targetId)).thenReturn(Optional.empty());
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.save(any(ApprovalStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(any())).thenReturn(List.of());
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(any())).thenReturn(List.of());
        when(workflowOutboxRepository.save(any(WorkflowOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approvalService.create(new CreateApprovalCommand(
                ApprovalTargetType.BOOKING,
                targetId,
                requesterId,
                null,
                managerId,
                resourceId,
                "Booking approval",
                "RESOURCE_BOOKING_APPROVAL",
                "Needs manager review",
                null,
                null,
                null
        ));

        ArgumentCaptor<ApprovalRequestEntity> requestCaptor = ArgumentCaptor.forClass(ApprovalRequestEntity.class);
        verify(approvalRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getApproverId()).isEqualTo(managerId);
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(requestCaptor.getValue().getApprovalScope()).isEqualTo(ApprovalScope.ASSIGNED_USER);
        assertThat(response.status()).isEqualTo("PENDING");

        ArgumentCaptor<ApprovalStepEntity> stepCaptor = ArgumentCaptor.forClass(ApprovalStepEntity.class);
        verify(approvalStepRepository).save(stepCaptor.capture());
        assertThat(stepCaptor.getValue().getStatus()).isEqualTo(ApprovalStepStatus.PENDING);
        assertThat(stepCaptor.getValue().getStepNumber()).isEqualTo(1);

        ArgumentCaptor<ApprovalDecisionHistoryEntity> historyCaptor = ArgumentCaptor.forClass(ApprovalDecisionHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(ApprovalAction.CREATED);

        ArgumentCaptor<WorkflowOutboxEntity> outboxCaptor = ArgumentCaptor.forClass(WorkflowOutboxEntity.class);
        verify(workflowOutboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("workflow.approval.created");
    }

    @Test
    void createShouldMarkAdminScopeWhenResourcePolicyRequiresAdminApproval() {
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(resourceApprovalPolicyGateway.resolve(resourceId))
                .thenReturn(new ResourceApprovalPolicyGateway.ResourceApprovalPolicy(resourceId, managerId, "ADMIN_APPROVAL", true));
        when(approvalRequestRepository.findByTargetTypeAndTargetId(ApprovalTargetType.BOOKING, targetId)).thenReturn(Optional.empty());
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.save(any(ApprovalStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(any())).thenReturn(List.of());
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(any())).thenReturn(List.of());
        when(workflowOutboxRepository.save(any(WorkflowOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approvalService.create(new CreateApprovalCommand(
                ApprovalTargetType.BOOKING,
                targetId,
                requesterId,
                null,
                managerId,
                resourceId,
                "Booking approval",
                null,
                "Needs admin review",
                null,
                null,
                null
        ));

        assertThat(response.approvalScope()).isEqualTo("ADMIN_ONLY");
        assertThat(response.approvalType()).isEqualTo("RESOURCE_BOOKING_ADMIN_APPROVAL");
    }

    @Test
    void createShouldBuildMultiStepApprovalWhenAdditionalApproversExist() {
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID approver1 = UUID.randomUUID();
        UUID approver2 = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(approvalRequestRepository.findByTargetTypeAndTargetId(ApprovalTargetType.BOOKING, targetId)).thenReturn(Optional.empty());
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.save(any(ApprovalStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(any())).thenReturn(List.of());
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(any())).thenReturn(List.of());
        when(workflowOutboxRepository.save(any(WorkflowOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approvalService.create(new CreateApprovalCommand(
                ApprovalTargetType.BOOKING,
                targetId,
                requesterId,
                approver1,
                approver1,
                resourceId,
                "Booking approval",
                "RESOURCE_BOOKING_APPROVAL",
                "Needs two-step approval",
                null,
                null,
                List.of(approver2)
        ));

        ArgumentCaptor<ApprovalStepEntity> stepCaptor = ArgumentCaptor.forClass(ApprovalStepEntity.class);
        verify(approvalStepRepository, times(2)).save(stepCaptor.capture());
        assertThat(stepCaptor.getAllValues().get(0).getStatus()).isEqualTo(ApprovalStepStatus.PENDING);
        assertThat(stepCaptor.getAllValues().get(1).getStatus()).isEqualTo(ApprovalStepStatus.WAITING);
        assertThat(response.currentStep()).isEqualTo(1);
        assertThat(response.totalSteps()).isEqualTo(2);
    }

    @Test
    void approveShouldAdvanceToNextStepWithoutFinalCallback() {
        UUID approvalId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID approver1 = UUID.randomUUID();
        UUID approver2 = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, bookingId, approver1, ApprovalStatus.PENDING, ApprovalScope.ASSIGNED_USER);
        entity.setTotalSteps(2);
        ApprovalStepEntity current = pendingStep(approvalId, approver1);
        ApprovalStepEntity next = waitingStep(approvalId, 2, approver2);

        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByApprovalIdAndStepNumber(approvalId, 1)).thenReturn(Optional.of(current));
        when(approvalStepRepository.findByApprovalIdAndStepNumber(approvalId, 2)).thenReturn(Optional.of(next));
        when(approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(approvalId)).thenReturn(List.of(current, next));
        when(approvalStepRepository.save(any(ApprovalStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(approvalId)).thenReturn(List.of());
        when(workflowOutboxRepository.save(any(WorkflowOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approvalService.applyDecision(approvalId, approver1, false, "step one approved", ApprovalDecisionType.APPROVE);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.currentStep()).isEqualTo(2);
        assertThat(response.approverId()).isEqualTo(approver2);
        assertThat(bookingWorkflowClient.lastDecisionBookingId).isNull();

        ArgumentCaptor<WorkflowOutboxEntity> outboxCaptor = ArgumentCaptor.forClass(WorkflowOutboxEntity.class);
        verify(workflowOutboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("workflow.approval.step-approved");
    }

    @Test
    void approveShouldUpdateStepPersistOutboxAndCallbackBookingService() {
        UUID approvalId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, bookingId, approverId, ApprovalStatus.PENDING, ApprovalScope.ASSIGNED_USER);
        ApprovalStepEntity step = pendingStep(approvalId, approverId);

        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByApprovalIdAndStepNumber(approvalId, 1)).thenReturn(Optional.of(step));
        when(approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(approvalId)).thenReturn(List.of(step));
        when(approvalStepRepository.save(any(ApprovalStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(approvalId)).thenReturn(List.of());
        when(workflowOutboxRepository.save(any(WorkflowOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approvalService.applyDecision(approvalId, approverId, false, "approved", ApprovalDecisionType.APPROVE);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(bookingWorkflowClient.lastDecisionBookingId).isEqualTo(bookingId);
        assertThat(bookingWorkflowClient.lastDecision).isEqualTo("APPROVED");
        assertThat(bookingWorkflowClient.lastNote).isEqualTo("approved");

        ArgumentCaptor<ApprovalStepEntity> stepCaptor = ArgumentCaptor.forClass(ApprovalStepEntity.class);
        verify(approvalStepRepository).save(stepCaptor.capture());
        assertThat(stepCaptor.getValue().getStatus()).isEqualTo(ApprovalStepStatus.APPROVED);

        ArgumentCaptor<WorkflowOutboxEntity> outboxCaptor = ArgumentCaptor.forClass(WorkflowOutboxEntity.class);
        verify(workflowOutboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("workflow.approval.approved");
    }

    @Test
    void approveShouldCallbackEventServiceForEventTargets() {
        UUID approvalId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, eventId, approverId, ApprovalStatus.PENDING, ApprovalScope.ADMIN_ONLY);
        entity.setTargetType(ApprovalTargetType.EVENT);
        entity.setApprovalType("EVENT_PUBLICATION_APPROVAL");
        ApprovalStepEntity step = pendingStep(approvalId, approverId);

        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByApprovalIdAndStepNumber(approvalId, 1)).thenReturn(Optional.of(step));
        when(approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(approvalId)).thenReturn(List.of(step));
        when(approvalStepRepository.save(any(ApprovalStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(approvalId)).thenReturn(List.of());
        when(workflowOutboxRepository.save(any(WorkflowOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approvalService.applyDecision(approvalId, UUID.randomUUID(), true, "approved", ApprovalDecisionType.APPROVE);

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(eventWorkflowClient.lastDecisionEventId).isEqualTo(eventId);
        assertThat(eventWorkflowClient.lastDecision).isEqualTo("APPROVED");
    }

    @Test
    void rejectShouldUpdateStepPersistOutboxAndCallbackBookingService() {
        UUID approvalId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, bookingId, approverId, ApprovalStatus.PENDING, ApprovalScope.ASSIGNED_USER);
        ApprovalStepEntity step = pendingStep(approvalId, approverId);

        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByApprovalIdAndStepNumber(approvalId, 1)).thenReturn(Optional.of(step));
        when(approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(approvalId)).thenReturn(List.of(step));
        when(approvalStepRepository.save(any(ApprovalStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(approvalId)).thenReturn(List.of());
        when(workflowOutboxRepository.save(any(WorkflowOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approvalService.applyDecision(approvalId, approverId, false, "rejected", ApprovalDecisionType.REJECT);

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(bookingWorkflowClient.lastDecisionBookingId).isEqualTo(bookingId);
        assertThat(bookingWorkflowClient.lastDecision).isEqualTo("REJECTED");
        assertThat(bookingWorkflowClient.lastNote).isEqualTo("rejected");
    }

    @Test
    void approveShouldRejectAlreadyApprovedRequest() {
        UUID approvalId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, UUID.randomUUID(), approverId, ApprovalStatus.APPROVED, ApprovalScope.ASSIGNED_USER);
        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> approvalService.applyDecision(approvalId, approverId, false, "again", ApprovalDecisionType.APPROVE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(approvalRequestRepository, never()).save(any(ApprovalRequestEntity.class));
        verify(approvalStepRepository, never()).save(any(ApprovalStepEntity.class));
        verify(workflowOutboxRepository, never()).save(any(WorkflowOutboxEntity.class));
        assertThat(bookingWorkflowClient.lastDecisionBookingId).isNull();
    }

    @Test
    void byIdShouldRejectUnrelatedUser() {
        UUID approvalId = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, UUID.randomUUID(), UUID.randomUUID(), ApprovalStatus.PENDING, ApprovalScope.ASSIGNED_USER);
        entity.setRequesterId(UUID.randomUUID());
        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> approvalService.byId(approvalId, UUID.randomUUID(), false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void nonAdminApproverShouldBeBlockedForAdminOnlyApproval() {
        UUID approvalId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, UUID.randomUUID(), approverId, ApprovalStatus.PENDING, ApprovalScope.ADMIN_ONLY);
        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> approvalService.applyDecision(approvalId, approverId, false, "approve", ApprovalDecisionType.APPROVE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void createShouldSupportEventApprovalTemplate() {
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();

        when(approvalRequestRepository.findByTargetTypeAndTargetId(ApprovalTargetType.EVENT, targetId)).thenReturn(Optional.empty());
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.save(any(ApprovalStepEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalStepRepository.findByApprovalIdOrderByStepNumberAsc(any())).thenReturn(List.of());
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(any())).thenReturn(List.of());
        when(workflowOutboxRepository.save(any(WorkflowOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approvalService.create(new CreateApprovalCommand(
                ApprovalTargetType.EVENT,
                targetId,
                requesterId,
                approverId,
                approverId,
                null,
                "Event approval",
                null,
                "Needs admin event review",
                null,
                null,
                null
        ));

        assertThat(response.targetType()).isEqualTo("EVENT");
        assertThat(response.approvalType()).isEqualTo("EVENT_APPROVAL");
        assertThat(response.approvalScope()).isEqualTo("ADMIN_ONLY");
    }

    private ApprovalRequestEntity approvalEntity(UUID approvalId, UUID targetId, UUID approverId, ApprovalStatus status, ApprovalScope scope) {
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setApprovalId(approvalId);
        entity.setTargetType(ApprovalTargetType.BOOKING);
        entity.setTargetId(targetId);
        entity.setApprovalType("RESOURCE_BOOKING_APPROVAL");
        entity.setRequesterId(UUID.randomUUID());
        entity.setApproverId(approverId);
        entity.setApprovalScope(scope);
        entity.setTargetOwnerId(approverId);
        entity.setResourceId(UUID.randomUUID());
        entity.setTitle("Booking approval");
        entity.setSummary("summary");
        entity.setCurrentStep(1);
        entity.setTotalSteps(1);
        entity.setStatus(status);
        entity.setSubmittedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return entity;
    }

    private ApprovalStepEntity pendingStep(UUID approvalId, UUID approverId) {
        ApprovalStepEntity step = new ApprovalStepEntity();
        step.setStepId(UUID.randomUUID());
        step.setApprovalId(approvalId);
        step.setStepNumber(1);
        step.setApproverId(approverId);
        step.setStatus(ApprovalStepStatus.PENDING);
        step.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        step.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return step;
    }

    private ApprovalStepEntity waitingStep(UUID approvalId, int stepNumber, UUID approverId) {
        ApprovalStepEntity step = new ApprovalStepEntity();
        step.setStepId(UUID.randomUUID());
        step.setApprovalId(approvalId);
        step.setStepNumber(stepNumber);
        step.setApproverId(approverId);
        step.setStatus(ApprovalStepStatus.WAITING);
        step.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        step.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return step;
    }

    private static class StubBookingWorkflowClient extends BookingWorkflowClient {
        private UUID lastDecisionBookingId;
        private String lastDecision;
        private String lastNote;

        StubBookingWorkflowClient() {
            super(RestClient.create(), new ClientProperties(null, null, null, null, null, null, null));
        }

        @Override
        public void applyDecision(UUID bookingId, com.teamresource.workflow.api.dto.ApprovalDecisionCallbackRequest request) {
            this.lastDecisionBookingId = bookingId;
            this.lastDecision = request.decision();
            this.lastNote = request.note();
        }
    }

    private static class StubEventWorkflowClient extends EventWorkflowClient {
        private UUID lastDecisionEventId;
        private String lastDecision;
        private String lastNote;

        StubEventWorkflowClient() {
            super(RestClient.create(), new ClientProperties(null, null, null, null, null, null, null));
        }

        @Override
        public void applyDecision(UUID eventId, ApprovalDecisionPayload request) {
            this.lastDecisionEventId = eventId;
            this.lastDecision = request.decision();
            this.lastNote = request.note();
        }
    }
}
