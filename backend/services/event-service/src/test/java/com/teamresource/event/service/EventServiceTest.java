package com.teamresource.event.service;

import com.teamresource.event.api.dto.CreateEventRequest;
import com.teamresource.event.api.dto.UpdateEventRequest;
import com.teamresource.event.domain.EventCategory;
import com.teamresource.event.domain.EventStatus;
import com.teamresource.event.infra.persistence.EventEntity;
import com.teamresource.event.infra.persistence.EventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void createShouldPersistDraftEvent() {
        UUID organizerId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        OffsetDateTime endAt = startAt.plusHours(2);

        when(eventRepository.save(any(EventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = eventService.create(organizerId, new CreateEventRequest(
                " Spring Workshop ",
                "Workshop description",
                "workshop",
                "Room 1",
                40,
                startAt.minusDays(1),
                startAt.minusHours(1),
                startAt,
                endAt
        ));

        ArgumentCaptor<EventEntity> eventCaptor = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        EventEntity saved = eventCaptor.getValue();

        assertThat(saved.getOrganizerId()).isEqualTo(organizerId);
        assertThat(saved.getTitle()).isEqualTo("Spring Workshop");
        assertThat(saved.getCategory()).isEqualTo(EventCategory.WORKSHOP);
        assertThat(saved.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(response.status()).isEqualTo("DRAFT");
    }

    @Test
    void updateShouldRejectCancelledEvents() {
        UUID eventId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        EventEntity entity = new EventEntity();
        entity.setEventId(eventId);
        entity.setOrganizerId(organizerId);
        entity.setStatus(EventStatus.CANCELLED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(entity));

        OffsetDateTime startAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(3);
        OffsetDateTime endAt = startAt.plusHours(1);

        assertThatThrownBy(() -> eventService.update(eventId, organizerId, false, new UpdateEventRequest(
                "Updated title",
                "Updated description",
                "MEETING",
                "Room 2",
                20,
                startAt.minusDays(1),
                startAt.minusHours(2),
                startAt,
                endAt
        )))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void publishShouldRejectNonOwnerWithoutAdminRole() {
        UUID eventId = UUID.randomUUID();
        EventEntity entity = new EventEntity();
        entity.setEventId(eventId);
        entity.setOrganizerId(UUID.randomUUID());
        entity.setStatus(EventStatus.DRAFT);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> eventService.publish(eventId, UUID.randomUUID(), false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
