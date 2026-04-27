package com.teamresource.analytics.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "resource_popularity", schema = "analytics")
public class ResourcePopularityEntity {

    @Id
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "resource_name", nullable = false, length = 160)
    private String resourceName;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Column(name = "total_bookings", nullable = false)
    private long totalBookings;

    @Column(name = "approved_bookings", nullable = false)
    private long approvedBookings;

    @Column(name = "pending_bookings", nullable = false)
    private long pendingBookings;

    @Column(name = "waitlisted_bookings", nullable = false)
    private long waitlistedBookings;

    @Column(name = "cancelled_bookings", nullable = false)
    private long cancelledBookings;

    @Column(name = "total_reserved_minutes", nullable = false)
    private long totalReservedMinutes;

    @Column(name = "popularity_score", nullable = false, precision = 12, scale = 2)
    private BigDecimal popularityScore;

    @Column(name = "last_refreshed_at", nullable = false)
    private OffsetDateTime lastRefreshedAt;

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }
    public long getApprovedBookings() { return approvedBookings; }
    public void setApprovedBookings(long approvedBookings) { this.approvedBookings = approvedBookings; }
    public long getPendingBookings() { return pendingBookings; }
    public void setPendingBookings(long pendingBookings) { this.pendingBookings = pendingBookings; }
    public long getWaitlistedBookings() { return waitlistedBookings; }
    public void setWaitlistedBookings(long waitlistedBookings) { this.waitlistedBookings = waitlistedBookings; }
    public long getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(long cancelledBookings) { this.cancelledBookings = cancelledBookings; }
    public long getTotalReservedMinutes() { return totalReservedMinutes; }
    public void setTotalReservedMinutes(long totalReservedMinutes) { this.totalReservedMinutes = totalReservedMinutes; }
    public BigDecimal getPopularityScore() { return popularityScore; }
    public void setPopularityScore(BigDecimal popularityScore) { this.popularityScore = popularityScore; }
    public OffsetDateTime getLastRefreshedAt() { return lastRefreshedAt; }
    public void setLastRefreshedAt(OffsetDateTime lastRefreshedAt) { this.lastRefreshedAt = lastRefreshedAt; }
}
