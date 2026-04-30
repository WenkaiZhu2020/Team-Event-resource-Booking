package com.teamresource.analytics.service;

import com.teamresource.analytics.api.dto.EventRegistrationMetricResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.api.dto.ResourceUsageMetricResponse;
import com.teamresource.analytics.domain.BookingAnalyticsStatus;
import com.teamresource.analytics.infra.persistence.BookingFactEntity;
import com.teamresource.analytics.infra.persistence.BookingFactRepository;
import com.teamresource.analytics.infra.persistence.ResourcePopularityEntity;
import com.teamresource.analytics.infra.persistence.ResourcePopularityRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardQueryServiceTest {

    private static final OffsetDateTime DEFAULT_FROM = OffsetDateTime.parse("2000-01-01T00:00:00Z");
    private static final OffsetDateTime DEFAULT_TO = OffsetDateTime.parse("2100-01-01T00:00:00Z");

    @Test
    void queryMethodsShouldDelegateAndMapResults() {
        BookingFactRepository bookingFactRepository = mock(BookingFactRepository.class);
        ResourcePopularityRepository resourcePopularityRepository = mock(ResourcePopularityRepository.class);
        DashboardQueryService service = new DashboardQueryService(bookingFactRepository, resourcePopularityRepository);
        OffsetDateTime from = OffsetDateTime.parse("2026-04-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-04-30T23:59:59Z");

        when(bookingFactRepository.countAll(from, to)).thenReturn(12L);
        when(bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.APPROVED, from, to)).thenReturn(7L);
        when(bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.PENDING_APPROVAL, from, to)).thenReturn(2L);
        when(bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.WAITLISTED, from, to)).thenReturn(1L);
        when(bookingFactRepository.countByBookingStatusIn(any(), eq(from), eq(to))).thenReturn(2L);
        when(bookingFactRepository.countDistinctResourcesUsed(any(), eq(from), eq(to))).thenReturn(5L);
        when(bookingFactRepository.countApprovedBookingsBetween(from, to)).thenReturn(3L);
        when(bookingFactRepository.sumApprovedReservedMinutes(from, to)).thenReturn(840L);

        BookingFactRepository.EventRegistrationProjection eventProjection = new BookingFactRepository.EventRegistrationProjection() {
            @Override public UUID getEventId() { return UUID.fromString("11111111-1111-1111-1111-111111111111"); }
            @Override public long getActiveBookings() { return 4; }
            @Override public long getWaitlistedBookings() { return 1; }
            @Override public long getCancelledBookings() { return 2; }
        };
        BookingFactRepository.ResourceUsageProjection usageProjection = new BookingFactRepository.ResourceUsageProjection() {
            @Override public UUID getResourceId() { return UUID.fromString("22222222-2222-2222-2222-222222222222"); }
            @Override public long getTotalBookings() { return 6; }
            @Override public long getApprovedBookings() { return 4; }
            @Override public long getPendingBookings() { return 1; }
            @Override public long getCancelledBookings() { return 1; }
            @Override public long getBookedMinutes() { return 300; }
        };
        when(bookingFactRepository.aggregateEventRegistrations(from, to, 10)).thenReturn(List.of(eventProjection));
        when(bookingFactRepository.aggregateResourceUsage(from, to, 10)).thenReturn(List.of(usageProjection));

        ResourcePopularityEntity popularityEntity = new ResourcePopularityEntity();
        popularityEntity.setResourceId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        popularityEntity.setResourceName("Room A");
        popularityEntity.setResourceType("ROOM");
        popularityEntity.setTotalBookings(10);
        popularityEntity.setApprovedBookings(8);
        popularityEntity.setPendingBookings(1);
        popularityEntity.setWaitlistedBookings(1);
        popularityEntity.setCancelledBookings(0);
        popularityEntity.setTotalReservedMinutes(500);
        popularityEntity.setPopularityScore(BigDecimal.valueOf(123.45));
        popularityEntity.setLastRefreshedAt(to);
        when(resourcePopularityRepository.findAllByOrderByPopularityScoreDesc(PageRequest.of(0, 10)))
                .thenReturn(List.of(popularityEntity));

        assertThat(service.totalBookings(from, to)).isEqualTo(12L);
        assertThat(service.approvedBookings(from, to)).isEqualTo(7L);
        assertThat(service.pendingApprovalBookings(from, to)).isEqualTo(2L);
        assertThat(service.waitlistedBookings(from, to)).isEqualTo(1L);
        assertThat(service.cancelledOrRejectedBookings(from, to)).isEqualTo(2L);
        assertThat(service.uniqueResourcesUsed(from, to)).isEqualTo(5L);
        assertThat(service.nextSevenDaysApprovedBookings(from, to)).isEqualTo(3L);
        assertThat(service.totalApprovedReservedMinutes(from, to)).isEqualTo(840L);

        List<EventRegistrationMetricResponse> eventMetrics = service.eventRegistrationMetrics(from, to, 10);
        List<ResourceUsageMetricResponse> usageMetrics = service.resourceUsageMetrics(from, to, 10);
        List<ResourcePopularityResponse> topResources = service.topResources(10);

        assertThat(eventMetrics).singleElement().satisfies(metric -> {
            assertThat(metric.activeBookings()).isEqualTo(4);
            assertThat(metric.waitlistedBookings()).isEqualTo(1);
        });
        assertThat(usageMetrics).singleElement().satisfies(metric -> {
            assertThat(metric.totalBookings()).isEqualTo(6);
            assertThat(metric.bookedMinutes()).isEqualTo(300);
        });
        assertThat(topResources).singleElement().satisfies(resource -> {
            assertThat(resource.resourceName()).isEqualTo("Room A");
            assertThat(resource.popularityScore()).isEqualByComparingTo("123.45");
        });
    }

    @Test
    void convenienceMethodsShouldUseDefaultFilters() {
        BookingFactRepository bookingFactRepository = mock(BookingFactRepository.class);
        ResourcePopularityRepository resourcePopularityRepository = mock(ResourcePopularityRepository.class);
        DashboardQueryService service = new DashboardQueryService(bookingFactRepository, resourcePopularityRepository);

        when(bookingFactRepository.countAll(DEFAULT_FROM, DEFAULT_TO)).thenReturn(1L);
        when(bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.APPROVED, DEFAULT_FROM, DEFAULT_TO)).thenReturn(2L);
        when(bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.PENDING_APPROVAL, DEFAULT_FROM, DEFAULT_TO)).thenReturn(3L);
        when(bookingFactRepository.countByBookingStatus(BookingAnalyticsStatus.WAITLISTED, DEFAULT_FROM, DEFAULT_TO)).thenReturn(4L);
        when(bookingFactRepository.countByBookingStatusIn(any(), eq(DEFAULT_FROM), eq(DEFAULT_TO))).thenReturn(5L);
        when(bookingFactRepository.countDistinctResourcesUsed(any(), eq(DEFAULT_FROM), eq(DEFAULT_TO))).thenReturn(6L);
        when(bookingFactRepository.countApprovedBookingsBetween(any(), any())).thenReturn(7L);
        when(bookingFactRepository.sumApprovedReservedMinutes(DEFAULT_FROM, DEFAULT_TO)).thenReturn(8L);
        when(resourcePopularityRepository.findAllByOrderByPopularityScoreDesc(PageRequest.of(0, 9))).thenReturn(List.of());

        assertThat(service.totalBookings()).isEqualTo(1L);
        assertThat(service.approvedBookings()).isEqualTo(2L);
        assertThat(service.pendingApprovalBookings()).isEqualTo(3L);
        assertThat(service.waitlistedBookings()).isEqualTo(4L);
        assertThat(service.cancelledOrRejectedBookings()).isEqualTo(5L);
        assertThat(service.uniqueResourcesUsed()).isEqualTo(6L);
        assertThat(service.nextSevenDaysApprovedBookings()).isEqualTo(7L);
        assertThat(service.totalApprovedReservedMinutes()).isEqualTo(8L);
        assertThat(service.topResources(9)).isEmpty();

        verify(bookingFactRepository).countAll(DEFAULT_FROM, DEFAULT_TO);
    }

    @Test
    void bookingEventAggregationStrategyShouldUpsertAndHandleFallbacks() {
        BookingFactRepository bookingFactRepository = mock(BookingFactRepository.class);
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.teamresource.analytics.service.strategy.BookingEventAggregationStrategy strategy =
                new com.teamresource.analytics.service.strategy.BookingEventAggregationStrategy(bookingFactRepository);

        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-04-30T12:00:00Z");
        var payload = objectMapper.valueToTree(java.util.Map.ofEntries(
                java.util.Map.entry("bookingId", bookingId.toString()),
                java.util.Map.entry("userId", userId.toString()),
                java.util.Map.entry("linkedEventId", eventId.toString()),
                java.util.Map.entry("resourceId", resourceId.toString()),
                java.util.Map.entry("resourceName", "Room B"),
                java.util.Map.entry("resourceType", "ROOM"),
                java.util.Map.entry("status", "approved"),
                java.util.Map.entry("approvalMode", "AUTO"),
                java.util.Map.entry("waitlistPosition", 2),
                java.util.Map.entry("startAt", "2026-05-01T10:00:00Z"),
                java.util.Map.entry("endAt", "2026-05-01T12:00:00Z"),
                java.util.Map.entry("createdAt", "2026-04-01T10:00:00Z"),
                java.util.Map.entry("updatedAt", "2026-04-15T10:00:00Z")
        ));
        var eventMessage = new com.teamresource.analytics.infra.messaging.DomainEventMessage(
                UUID.randomUUID(), "booking", bookingId, "booking.created", payload, occurredAt);

        when(bookingFactRepository.findById(bookingId)).thenReturn(Optional.empty());
        when(bookingFactRepository.save(any(BookingFactEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(strategy.supports(eventMessage)).isTrue();
        strategy.apply(eventMessage);

        verify(bookingFactRepository).save(any(BookingFactEntity.class));

        var invalidPayload = objectMapper.valueToTree(java.util.Map.of("bookingId", "not-a-uuid"));
        strategy.apply(new com.teamresource.analytics.infra.messaging.DomainEventMessage(
                UUID.randomUUID(), "booking", bookingId, "booking.updated", invalidPayload, null));

        assertThat(strategy.supports(new com.teamresource.analytics.infra.messaging.DomainEventMessage(
                UUID.randomUUID(), "booking", bookingId, "workflow.changed", payload, occurredAt))).isFalse();
        assertThat(strategy.supports(new com.teamresource.analytics.infra.messaging.DomainEventMessage(
                UUID.randomUUID(), "booking", bookingId, null, payload, occurredAt))).isFalse();
    }

    @Test
    void bookingEventAggregationStrategyShouldHandleExistingEntityAndFallbackValues() {
        BookingFactRepository bookingFactRepository = mock(BookingFactRepository.class);
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.teamresource.analytics.service.strategy.BookingEventAggregationStrategy strategy =
                new com.teamresource.analytics.service.strategy.BookingEventAggregationStrategy(bookingFactRepository);
        BookingFactEntity existing = new BookingFactEntity();
        UUID bookingId = UUID.randomUUID();
        existing.setBookingId(bookingId);
        when(bookingFactRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(bookingFactRepository.save(any(BookingFactEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var payload = objectMapper.valueToTree(java.util.Map.ofEntries(
                java.util.Map.entry("bookingId", bookingId.toString()),
                java.util.Map.entry("status", "not-a-real-status"),
                java.util.Map.entry("startAt", "bad-time"),
                java.util.Map.entry("endAt", "bad-time"),
                java.util.Map.entry("createdAt", "bad-time"),
                java.util.Map.entry("updatedAt", "bad-time")
        ));

        strategy.apply(new com.teamresource.analytics.infra.messaging.DomainEventMessage(
                UUID.randomUUID(), "booking", bookingId, "booking.changed", payload, null));

        assertThat(existing.getBookingStatus()).isEqualTo(BookingAnalyticsStatus.UNKNOWN);
        assertThat(existing.getResourceName()).isEqualTo("Unknown resource");
        assertThat(existing.getResourceType()).isEqualTo("UNKNOWN");
        assertThat(existing.getApprovalMode()).isEqualTo("UNKNOWN");
        assertThat(existing.getWaitlistPosition()).isNull();
        assertThat(existing.getStartAt()).isNull();
        assertThat(existing.getEndAt()).isNull();
        assertThat(existing.getCreatedAt()).isNotNull();
        assertThat(existing.getUpdatedAt()).isNotNull();
        assertThat(existing.getLastEventAt()).isNotNull();

        assertThat(invokePrivate(strategy, "uuid", new Class[]{com.fasterxml.jackson.databind.JsonNode.class},
                new Object[]{com.fasterxml.jackson.databind.node.NullNode.getInstance()})).isNull();
        assertThat(invokePrivate(strategy, "integer", new Class[]{com.fasterxml.jackson.databind.JsonNode.class},
                new Object[]{com.fasterxml.jackson.databind.node.NullNode.getInstance()})).isNull();
        assertThat(invokePrivate(strategy, "time", new Class[]{com.fasterxml.jackson.databind.JsonNode.class},
                new Object[]{com.fasterxml.jackson.databind.node.NullNode.getInstance()})).isNull();
        assertThat(invokePrivate(strategy, "time", new Class[]{com.fasterxml.jackson.databind.JsonNode.class},
                new Object[]{null})).isNull();
        assertThat(invokePrivate(strategy, "text",
                new Class[]{com.fasterxml.jackson.databind.JsonNode.class, String.class},
                new Object[]{com.fasterxml.jackson.databind.node.NullNode.getInstance(), "fallback"})).isEqualTo("fallback");
    }

    private static Object invokePrivate(Object target, String name, Class<?>[] parameterTypes, Object[] args) {
        try {
            var method = target.getClass().getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
