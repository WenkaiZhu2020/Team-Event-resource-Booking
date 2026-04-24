package com.teamresource.resource.service;

import com.teamresource.resource.api.dto.AvailabilityRuleRequest;
import com.teamresource.resource.api.dto.AvailabilityRuleResponse;
import com.teamresource.resource.api.dto.MaintenanceSlotRequest;
import com.teamresource.resource.api.dto.MaintenanceSlotResponse;
import com.teamresource.resource.api.dto.ResourceResponse;
import com.teamresource.resource.api.dto.UpsertResourceRequest;
import com.teamresource.resource.domain.ApprovalMode;
import com.teamresource.resource.domain.MaintenanceStatus;
import com.teamresource.resource.domain.ResourceStatus;
import com.teamresource.resource.domain.ResourceType;
import com.teamresource.resource.infra.persistence.AvailabilityRuleEntity;
import com.teamresource.resource.infra.persistence.MaintenanceSlotEntity;
import com.teamresource.resource.infra.persistence.ResourceEntity;
import com.teamresource.resource.infra.persistence.ResourceRepository;
import com.teamresource.resource.service.policy.ApprovalPolicyStrategyFactory;
import com.teamresource.resource.service.policy.ResourcePolicyDefaults;
import com.teamresource.resource.service.policy.ResourcePolicyDefaultsFactory;
import com.teamresource.resource.service.specification.ResourceSpecifications;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourcePolicyDefaultsFactory resourcePolicyDefaultsFactory;
    private final ApprovalPolicyStrategyFactory approvalPolicyStrategyFactory;

    public ResourceService(
            ResourceRepository resourceRepository,
            ResourcePolicyDefaultsFactory resourcePolicyDefaultsFactory,
            ApprovalPolicyStrategyFactory approvalPolicyStrategyFactory
    ) {
        this.resourceRepository = resourceRepository;
        this.resourcePolicyDefaultsFactory = resourcePolicyDefaultsFactory;
        this.approvalPolicyStrategyFactory = approvalPolicyStrategyFactory;
    }

    @Transactional
    public ResourceResponse create(UUID managerId, boolean privileged, UpsertResourceRequest request) {
        requireManagerAccess(privileged);
        ResourceType type = parseType(request.type());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ResourceEntity entity = new ResourceEntity();
        entity.setResourceId(UUID.randomUUID());
        entity.setManagerId(managerId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setStatus(ResourceStatus.ACTIVE);
        applyResourceMutation(entity, request, type, now);
        return toResponse(resourceRepository.save(entity));
    }

    @Transactional
    public ResourceResponse update(UUID resourceId, UUID currentUserId, boolean admin, UpsertResourceRequest request) {
        ResourceEntity entity = findResource(resourceId);
        requireOwnerOrAdmin(entity, currentUserId, admin);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        applyResourceMutation(entity, request, parseType(request.type()), now);
        entity.setUpdatedAt(now);
        return toResponse(resourceRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listCatalog(String query, String type, String status, Boolean requiresApproval) {
        Specification<ResourceEntity> specification = ResourceSpecifications.visibleCatalog(
                query,
                parseType(type),
                parseStatus(status),
                requiresApproval
        );
        return resourceRepository.findAll(specification).stream()
                .sorted(Comparator.comparing(ResourceEntity::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> myResources(UUID managerId) {
        return resourceRepository.findByManagerIdOrderByCreatedAtDesc(managerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse byId(UUID resourceId) {
        return toResponse(findResource(resourceId));
    }

    @Transactional
    public ResourceResponse activate(UUID resourceId, UUID currentUserId, boolean admin) {
        ResourceEntity entity = findResource(resourceId);
        requireOwnerOrAdmin(entity, currentUserId, admin);
        entity.setStatus(ResourceStatus.ACTIVE);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toResponse(resourceRepository.save(entity));
    }

    @Transactional
    public ResourceResponse deactivate(UUID resourceId, UUID currentUserId, boolean admin) {
        ResourceEntity entity = findResource(resourceId);
        requireOwnerOrAdmin(entity, currentUserId, admin);
        entity.setStatus(ResourceStatus.INACTIVE);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toResponse(resourceRepository.save(entity));
    }

    @Transactional
    public MaintenanceSlotResponse addMaintenanceSlot(
            UUID resourceId,
            UUID currentUserId,
            boolean admin,
            MaintenanceSlotRequest request
    ) {
        ResourceEntity entity = findResource(resourceId);
        requireOwnerOrAdmin(entity, currentUserId, admin);
        validateMaintenanceWindow(request.startsAt(), request.endsAt());
        ensureNoMaintenanceOverlap(entity, request.startsAt(), request.endsAt());

        MaintenanceSlotEntity slot = new MaintenanceSlotEntity();
        slot.setMaintenanceSlotId(UUID.randomUUID());
        slot.setResource(entity);
        slot.setStartsAt(request.startsAt());
        slot.setEndsAt(request.endsAt());
        slot.setReason(request.reason().trim());
        slot.setStatus(MaintenanceStatus.SCHEDULED);
        slot.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.getMaintenanceSlots().add(slot);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        resourceRepository.save(entity);
        return toResponse(slot);
    }

    @Transactional
    public void removeMaintenanceSlot(UUID resourceId, UUID slotId, UUID currentUserId, boolean admin) {
        ResourceEntity entity = findResource(resourceId);
        requireOwnerOrAdmin(entity, currentUserId, admin);
        boolean removed = entity.getMaintenanceSlots().removeIf(slot -> slot.getMaintenanceSlotId().equals(slotId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance slot not found");
        }
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        resourceRepository.save(entity);
    }

    private void applyResourceMutation(
            ResourceEntity entity,
            UpsertResourceRequest request,
            ResourceType type,
            OffsetDateTime now
    ) {
        validateAvailabilityRules(request.availabilityRules());
        ResourcePolicyDefaults defaults = resourcePolicyDefaultsFactory.create(type);
        ApprovalMode approvalMode = resolveApprovalMode(request.approvalMode(), defaults);

        entity.setName(request.name().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setType(type);
        entity.setLocation(request.location().trim());
        entity.setCapacity(request.capacity());
        entity.setApprovalMode(approvalMode);
        entity.setAllowWaitlist(request.allowWaitlist() != null ? request.allowWaitlist() : defaults.allowWaitlist());
        entity.setMaxBookingDurationMinutes(
                request.maxBookingDurationMinutes() != null
                        ? request.maxBookingDurationMinutes()
                        : defaults.maxBookingDurationMinutes()
        );
        entity.setAdvanceBookingWindowDays(
                request.advanceBookingWindowDays() != null
                        ? request.advanceBookingWindowDays()
                        : defaults.advanceBookingWindowDays()
        );

        entity.getAvailabilityRules().clear();
        for (AvailabilityRuleRequest ruleRequest : request.availabilityRules()) {
            AvailabilityRuleEntity rule = new AvailabilityRuleEntity();
            rule.setAvailabilityRuleId(UUID.randomUUID());
            rule.setResource(entity);
            rule.setDayOfWeek(ruleRequest.dayOfWeek().shortValue());
            rule.setStartTime(ruleRequest.startTime());
            rule.setEndTime(ruleRequest.endTime());
            rule.setAvailable(Boolean.TRUE.equals(ruleRequest.available()));
            rule.setCreatedAt(now);
            entity.getAvailabilityRules().add(rule);
        }
    }

    private ResourceEntity findResource(UUID resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    }

    private void requireManagerAccess(boolean privileged) {
        if (!privileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resource manager or admin role is required");
        }
    }

    private void requireOwnerOrAdmin(ResourceEntity entity, UUID currentUserId, boolean admin) {
        if (!admin && !entity.getManagerId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resource access denied");
        }
    }

    private ResourceType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ResourceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid resource type");
        }
    }

    private ResourceStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ResourceStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid resource status");
        }
    }

    private ApprovalMode resolveApprovalMode(String raw, ResourcePolicyDefaults defaults) {
        if (raw == null || raw.isBlank()) {
            return defaults.approvalMode();
        }
        try {
            return ApprovalMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid approval mode");
        }
    }

    private void validateAvailabilityRules(List<AvailabilityRuleRequest> rules) {
        for (AvailabilityRuleRequest rule : rules) {
            if (!rule.endTime().isAfter(rule.startTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Availability end time must be after start time");
            }
        }
    }

    private void validateMaintenanceWindow(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maintenance end time must be after start time");
        }
    }

    private void ensureNoMaintenanceOverlap(ResourceEntity entity, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        boolean overlaps = entity.getMaintenanceSlots().stream()
                .filter(slot -> slot.getStatus() == MaintenanceStatus.SCHEDULED)
                .anyMatch(slot -> startsAt.isBefore(slot.getEndsAt()) && endsAt.isAfter(slot.getStartsAt()));
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Maintenance slot overlaps with an existing window");
        }
    }

    private ResourceResponse toResponse(ResourceEntity entity) {
        return new ResourceResponse(
                entity.getResourceId(),
                entity.getManagerId(),
                entity.getName(),
                entity.getDescription(),
                entity.getType().name(),
                entity.getLocation(),
                entity.getCapacity(),
                entity.getStatus().name(),
                entity.getApprovalMode().name(),
                approvalPolicyStrategyFactory.get(entity.getApprovalMode()).requiresApproval(),
                entity.isAllowWaitlist(),
                entity.getMaxBookingDurationMinutes(),
                entity.getAdvanceBookingWindowDays(),
                entity.getAvailabilityRules().stream().map(this::toResponse).toList(),
                entity.getMaintenanceSlots().stream().map(this::toResponse).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AvailabilityRuleResponse toResponse(AvailabilityRuleEntity entity) {
        return new AvailabilityRuleResponse(
                entity.getAvailabilityRuleId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isAvailable()
        );
    }

    private MaintenanceSlotResponse toResponse(MaintenanceSlotEntity entity) {
        return new MaintenanceSlotResponse(
                entity.getMaintenanceSlotId(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getReason(),
                entity.getStatus().name()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
