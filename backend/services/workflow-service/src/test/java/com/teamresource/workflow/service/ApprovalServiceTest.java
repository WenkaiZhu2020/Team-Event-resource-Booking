package com.teamresource.workflow.service;

import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import com.teamresource.workflow.config.ClientProperties;
import com.teamresource.workflow.domain.ApprovalAction;
import com.teamresource.workflow.domain.ApprovalStatus;
import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.infra.client.BookingWorkflowClient;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryEntity;
import com.teamresource.workflow.infra.persistence.ApprovalDecisionHistoryRepository;
import com.teamresource.workflow.infra.persistence.ApprovalRequestEntity;
import com.teamresource.workflow.infra.persistence.ApprovalRequestRepository;
import com.teamresource.workflow.service.assignment.ApproverResolverChain;
import com.teamresource.workflow.service.assignment.ExplicitApproverResolver;
import com.teamresource.workflow.service.assignment.TargetOwnerApproverResolver;
import com.teamresource.workflow.service.template.BookingApprovalWorkflowTemplate;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private ApprovalDecisionHistoryRepository historyRepository;

    private StubBookingWorkflowClient bookingWorkflowClient;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        bookingWorkflowClient = new StubBookingWorkflowClient();
        approvalService = new ApprovalService(
                approvalRequestRepository,
                historyRepository,
                new BookingApprovalWorkflowTemplate(),
                new ApproverResolverChain(List.of(new ExplicitApproverResolver(), new TargetOwnerApproverResolver())),
                bookingWorkflowClient
        );
    }

    @Test
    void createShouldPersistBookingApprovalAndRecordHistory() {
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        when(approvalRequestRepository.findByTargetTypeAndTargetId(ApprovalTargetType.BOOKING, targetId)).thenReturn(Optional.empty());
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(any())).thenReturn(List.of());

        var response = approvalService.create(new CreateApprovalRequest(
                ApprovalTargetType.BOOKING,
                targetId,
                requesterId,
                null,
                managerId,
                UUID.randomUUID(),
                "Booking approval",
                "RESOURCE_BOOKING_APPROVAL",
                "Needs manager review"
        ));

        ArgumentCaptor<ApprovalRequestEntity> captor = ArgumentCaptor.forClass(ApprovalRequestEntity.class);
        verify(approvalRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getApproverId()).isEqualTo(managerId);
        assertThat(captor.getValue().getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(response.status()).isEqualTo("PENDING");

        ArgumentCaptor<ApprovalDecisionHistoryEntity> historyCaptor = ArgumentCaptor.forClass(ApprovalDecisionHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(ApprovalAction.CREATED);
    }

    @Test
    void approveShouldCallbackBookingService() {
        UUID approvalId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, bookingId, approverId);

        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByApprovalIdOrderByActedAtAsc(approvalId)).thenReturn(List.of());

        var response = approvalService.approve(new ApprovalDecisionCommand(approvalId, approverId, false, "approved"));

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(bookingWorkflowClient.lastDecisionBookingId).isEqualTo(bookingId);
        verify(historyRepository).save(any(ApprovalDecisionHistoryEntity.class));
    }

    @Test
    void byIdShouldRejectUnrelatedUser() {
        UUID approvalId = UUID.randomUUID();
        ApprovalRequestEntity entity = approvalEntity(approvalId, UUID.randomUUID(), UUID.randomUUID());
        entity.setRequesterId(UUID.randomUUID());
        when(approvalRequestRepository.findById(approvalId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> approvalService.byId(approvalId, UUID.randomUUID(), false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private ApprovalRequestEntity approvalEntity(UUID approvalId, UUID targetId, UUID approverId) {
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setApprovalId(approvalId);
        entity.setTargetType(ApprovalTargetType.BOOKING);
        entity.setTargetId(targetId);
        entity.setApprovalType("RESOURCE_BOOKING_APPROVAL");
        entity.setRequesterId(UUID.randomUUID());
        entity.setApproverId(approverId);
        entity.setTargetOwnerId(approverId);
        entity.setResourceId(UUID.randomUUID());
        entity.setTitle("Booking approval");
        entity.setSummary("summary");
        entity.setCurrentStep(1);
        entity.setTotalSteps(1);
        entity.setStatus(ApprovalStatus.PENDING);
        entity.setSubmittedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return entity;
    }

    private static class StubBookingWorkflowClient extends BookingWorkflowClient {
        private UUID lastDecisionBookingId;

        StubBookingWorkflowClient() {
            super(RestClient.create(), new ClientProperties(null, null, null));
        }

        @Override
        public void applyDecision(UUID bookingId, com.teamresource.workflow.api.dto.ApprovalDecisionCallbackRequest request) {
            this.lastDecisionBookingId = bookingId;
        }
    }
}
