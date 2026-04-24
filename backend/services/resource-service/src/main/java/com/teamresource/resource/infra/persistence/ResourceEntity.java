package com.teamresource.resource.infra.persistence;

import com.teamresource.resource.domain.ApprovalMode;
import com.teamresource.resource.domain.ResourceStatus;
import com.teamresource.resource.domain.ResourceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "resources", schema = "resources")
public class ResourceEntity {

    @Id
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "manager_id", nullable = false)
    private UUID managerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ResourceType type;

    @Column(nullable = false, length = 180)
    private String location;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ResourceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_mode", nullable = false, length = 32)
    private ApprovalMode approvalMode;

    @Column(name = "allow_waitlist", nullable = false)
    private boolean allowWaitlist;

    @Column(name = "max_booking_duration_minutes", nullable = false)
    private int maxBookingDurationMinutes;

    @Column(name = "advance_booking_window_days", nullable = false)
    private int advanceBookingWindowDays;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC, startTime ASC")
    private List<AvailabilityRuleEntity> availabilityRules = new ArrayList<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startsAt ASC")
    private List<MaintenanceSlotEntity> maintenanceSlots = new ArrayList<>();

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public UUID getManagerId() {
        return managerId;
    }

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public ApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(ApprovalMode approvalMode) {
        this.approvalMode = approvalMode;
    }

    public boolean isAllowWaitlist() {
        return allowWaitlist;
    }

    public void setAllowWaitlist(boolean allowWaitlist) {
        this.allowWaitlist = allowWaitlist;
    }

    public int getMaxBookingDurationMinutes() {
        return maxBookingDurationMinutes;
    }

    public void setMaxBookingDurationMinutes(int maxBookingDurationMinutes) {
        this.maxBookingDurationMinutes = maxBookingDurationMinutes;
    }

    public int getAdvanceBookingWindowDays() {
        return advanceBookingWindowDays;
    }

    public void setAdvanceBookingWindowDays(int advanceBookingWindowDays) {
        this.advanceBookingWindowDays = advanceBookingWindowDays;
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

    public Long getVersion() {
        return version;
    }

    public List<AvailabilityRuleEntity> getAvailabilityRules() {
        return availabilityRules;
    }

    public List<MaintenanceSlotEntity> getMaintenanceSlots() {
        return maintenanceSlots;
    }
}
