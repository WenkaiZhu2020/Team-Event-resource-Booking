package com.teamresource.event.service;

import com.teamresource.event.domain.EventRegistrationStatus;
import com.teamresource.event.domain.EventStatus;
import com.teamresource.event.infra.persistence.EventEntity;
import com.teamresource.event.infra.persistence.EventRegistrationEntity;
import com.teamresource.event.infra.persistence.EventRegistrationRepository;
import com.teamresource.event.infra.persistence.EventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @InjectMocks
    private EventRegistrationService eventRegistrationService;

    @Test
    void registerShouldUseSeatWhenCapacityAvailable() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EventEntity event = publishedEvent(eventId, 2, 0, 0);

        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());
        when(eventRegistrationRepository.save(any(EventRegistrationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(EventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = eventRegistrationService.register(eventId, userId);

        assertThat(response.status()).isEqualTo("REGISTERED");
        assertThat(response.waitlistPosition()).isNull();
        assertThat(event.getAttendeeProjectedCount()).isEqualTo(1);
        assertThat(event.getWaitlistProjectedCount()).isZero();
    }

    @Test
    void registerShouldWaitlistWhenCapacityIsFull() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EventEntity event = publishedEvent(eventId, 1, 1, 0);

        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());
        when(eventRegistrationRepository.maxWaitlistPosition(eventId)).thenReturn(0);
        when(eventRegistrationRepository.save(any(EventRegistrationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(EventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = eventRegistrationService.register(eventId, userId);

        assertThat(response.status()).isEqualTo("WAITLISTED");
        assertThat(response.waitlistPosition()).isEqualTo(1);
        assertThat(event.getAttendeeProjectedCount()).isEqualTo(1);
        assertThat(event.getWaitlistProjectedCount()).isEqualTo(1);
    }

    @Test
    void cancelShouldPromoteNextWaitlistedRegistration() {
        UUID eventId = UUID.randomUUID();
        UUID cancellingUserId = UUID.randomUUID();
        UUID promotedUserId = UUID.randomUUID();
        EventEntity event = publishedEvent(eventId, 1, 1, 1);

        EventRegistrationEntity cancellingRegistration = registration(
                eventId,
                cancellingUserId,
                EventRegistrationStatus.REGISTERED,
                null
        );
        EventRegistrationEntity waitlistedRegistration = registration(
                eventId,
                promotedUserId,
                EventRegistrationStatus.WAITLISTED,
                1
        );

        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByEventIdAndUserId(eventId, cancellingUserId))
                .thenReturn(Optional.of(cancellingRegistration));
        when(eventRegistrationRepository.findByEventIdAndStatusOrderByWaitlistPositionAsc(
                eventId,
                EventRegistrationStatus.WAITLISTED
        )).thenReturn(List.of(waitlistedRegistration), List.of());
        when(eventRegistrationRepository.save(any(EventRegistrationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(EventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = eventRegistrationService.cancel(eventId, cancellingUserId);

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(cancellingRegistration.getCancelledAt()).isNotNull();
        assertThat(waitlistedRegistration.getStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);
        assertThat(waitlistedRegistration.getWaitlistPosition()).isNull();
        assertThat(event.getAttendeeProjectedCount()).isEqualTo(1);
        assertThat(event.getWaitlistProjectedCount()).isZero();
        verify(eventRegistrationRepository, times(2)).save(any(EventRegistrationEntity.class));
    }

    private EventEntity publishedEvent(UUID eventId, int capacity, int attendeeProjectedCount, int waitlistProjectedCount) {
        EventEntity event = new EventEntity();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        event.setEventId(eventId);
        event.setOrganizerId(UUID.randomUUID());
        event.setTitle("Event");
        event.setLocation("Hall");
        event.setCapacity(capacity);
        event.setAttendeeProjectedCount(attendeeProjectedCount);
        event.setWaitlistProjectedCount(waitlistProjectedCount);
        event.setStatus(EventStatus.PUBLISHED);
        event.setRegistrationOpenAt(now.minusDays(1));
        event.setRegistrationCloseAt(now.plusDays(1));
        event.setStartAt(now.plusDays(2));
        event.setEndAt(now.plusDays(2).plusHours(1));
        event.setCreatedAt(now.minusDays(2));
        event.setUpdatedAt(now.minusHours(1));
        return event;
    }

    private EventRegistrationEntity registration(
            UUID eventId,
            UUID userId,
            EventRegistrationStatus status,
            Integer waitlistPosition
    ) {
        EventRegistrationEntity entity = new EventRegistrationEntity();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setRegistrationId(UUID.randomUUID());
        entity.setEventId(eventId);
        entity.setUserId(userId);
        entity.setStatus(status);
        entity.setWaitlistPosition(waitlistPosition);
        entity.setRegisteredAt(now.minusMinutes(10));
        entity.setCreatedAt(now.minusMinutes(10));
        entity.setUpdatedAt(now.minusMinutes(10));
        return entity;
    }
}
