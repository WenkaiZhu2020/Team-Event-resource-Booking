package com.teamresource.resource.service;

import com.teamresource.resource.api.dto.AvailabilityRuleRequest;
import com.teamresource.resource.api.dto.MaintenanceSlotRequest;
import com.teamresource.resource.api.dto.UpsertResourceRequest;
import com.teamresource.resource.domain.ApprovalMode;
import com.teamresource.resource.domain.MaintenanceStatus;
import com.teamresource.resource.domain.ResourceStatus;
import com.teamresource.resource.domain.ResourceType;
import com.teamresource.resource.infra.persistence.MaintenanceSlotEntity;
import com.teamresource.resource.infra.persistence.ResourceEntity;
import com.teamresource.resource.infra.persistence.ResourceRepository;
import com.teamresource.resource.service.policy.AdminApprovalStrategy;
import com.teamresource.resource.service.policy.ApprovalPolicyStrategyFactory;
import com.teamresource.resource.service.policy.AutoApproveStrategy;
import com.teamresource.resource.service.policy.ManagerApprovalStrategy;
import com.teamresource.resource.service.policy.ResourcePolicyDefaultsFactory;
import java.time.LocalTime;
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
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    private final ResourcePolicyDefaultsFactory defaultsFactory = new ResourcePolicyDefaultsFactory();
    private final ApprovalPolicyStrategyFactory strategyFactory = new ApprovalPolicyStrategyFactory(List.of(
            new AutoApproveStrategy(),
            new ManagerApprovalStrategy(),
            new AdminApprovalStrategy()
    ));

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService(resourceRepository, defaultsFactory, strategyFactory);
    }

    @Test
    void createShouldApplyTypeDefaultsWhenPolicyFieldsAreMissing() {
        UUID managerId = UUID.randomUUID();
        when(resourceRepository.save(any(ResourceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = resourceService.create(managerId, true, new UpsertResourceRequest(
                "Main Hall",
                "Large shared hall",
                "facility",
                "North Building",
                300,
                null,
                null,
                null,
                null,
                List.of(new AvailabilityRuleRequest(1, LocalTime.of(9, 0), LocalTime.of(18, 0), true))
        ));

        ArgumentCaptor<ResourceEntity> resourceCaptor = ArgumentCaptor.forClass(ResourceEntity.class);
        verify(resourceRepository).save(resourceCaptor.capture());
        ResourceEntity saved = resourceCaptor.getValue();

        assertThat(saved.getType()).isEqualTo(ResourceType.FACILITY);
        assertThat(saved.getApprovalMode()).isEqualTo(ApprovalMode.ADMIN_APPROVAL);
        assertThat(saved.isAllowWaitlist()).isFalse();
        assertThat(saved.getMaxBookingDurationMinutes()).isEqualTo(720);
        assertThat(saved.getAdvanceBookingWindowDays()).isEqualTo(90);
        assertThat(response.requiresApproval()).isTrue();
    }

    @Test
    void createShouldRejectNonManagerUsers() {
        assertThatThrownBy(() -> resourceService.create(UUID.randomUUID(), false, new UpsertResourceRequest(
                "Meeting Room",
                null,
                "ROOM",
                "Floor 2",
                12,
                "AUTO_APPROVE",
                true,
                120,
                14,
                List.of(new AvailabilityRuleRequest(1, LocalTime.of(9, 0), LocalTime.of(17, 0), true))
        )))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void addMaintenanceSlotShouldRejectOverlap() {
        UUID resourceId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        ResourceEntity resource = new ResourceEntity();
        resource.setResourceId(resourceId);
        resource.setManagerId(managerId);
        resource.setStatus(ResourceStatus.ACTIVE);
        resource.setType(ResourceType.ROOM);
        resource.setApprovalMode(ApprovalMode.MANAGER_APPROVAL);
        resource.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        resource.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        MaintenanceSlotEntity existing = new MaintenanceSlotEntity();
        existing.setMaintenanceSlotId(UUID.randomUUID());
        existing.setResource(resource);
        existing.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        existing.setEndsAt(existing.getStartsAt().plusHours(2));
        existing.setReason("Scheduled maintenance");
        existing.setStatus(MaintenanceStatus.SCHEDULED);
        existing.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        resource.getMaintenanceSlots().add(existing);

        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> resourceService.addMaintenanceSlot(
                resourceId,
                managerId,
                false,
                new MaintenanceSlotRequest(
                        existing.getStartsAt().plusMinutes(30),
                        existing.getEndsAt().plusMinutes(30),
                        "Overlap window"
                )
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
