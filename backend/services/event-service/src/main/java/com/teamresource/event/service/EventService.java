package com.teamresource.event.service;

import com.teamresource.event.api.dto.CreateEventRequest;
import com.teamresource.event.api.dto.EventResponse;
import com.teamresource.event.api.dto.UpdateEventRequest;
import com.teamresource.event.domain.EventCategory;
import com.teamresource.event.domain.EventStatus;
import com.teamresource.event.infra.persistence.EventEntity;
import com.teamresource.event.infra.persistence.EventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventResponse create(UUID organizerId, CreateEventRequest request) {
        validateTimeline(request.registrationOpenAt(), request.registrationCloseAt(), request.startAt(), request.endAt());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        EventEntity entity = new EventEntity();
        entity.setEventId(UUID.randomUUID());
        entity.setOrganizerId(organizerId);
        entity.setTitle(request.title().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setCategory(parseCategory(request.category()));
        entity.setLocation(request.location().trim());
        entity.setCapacity(request.capacity());
        entity.setRegistrationOpenAt(request.registrationOpenAt());
        entity.setRegistrationCloseAt(request.registrationCloseAt());
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setStatus(EventStatus.DRAFT);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(eventRepository.save(entity));
    }

    @Transactional
    public EventResponse update(UUID eventId, UUID currentUserId, boolean admin, UpdateEventRequest request) {
        validateTimeline(request.registrationOpenAt(), request.registrationCloseAt(), request.startAt(), request.endAt());
        EventEntity entity = findEvent(eventId);
        requireOwnerOrAdmin(entity, currentUserId, admin);
        if (entity.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled events cannot be updated");
        }
        entity.setTitle(request.title().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setCategory(parseCategory(request.category()));
        entity.setLocation(request.location().trim());
        entity.setCapacity(request.capacity());
        entity.setRegistrationOpenAt(request.registrationOpenAt());
        entity.setRegistrationCloseAt(request.registrationCloseAt());
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toResponse(eventRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public EventResponse byId(UUID eventId) {
        return toResponse(findEvent(eventId));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> publishedEvents() {
        return eventRepository.findByStatusOrderByStartAtAsc(EventStatus.PUBLISHED).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> myEvents(UUID organizerId) {
        return eventRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventResponse publish(UUID eventId, UUID currentUserId, boolean admin) {
        EventEntity entity = findEvent(eventId);
        requireOwnerOrAdmin(entity, currentUserId, admin);
        if (entity.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled events cannot be published");
        }
        entity.setStatus(EventStatus.PUBLISHED);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toResponse(eventRepository.save(entity));
    }

    @Transactional
    public EventResponse cancel(UUID eventId, UUID currentUserId, boolean admin) {
        EventEntity entity = findEvent(eventId);
        requireOwnerOrAdmin(entity, currentUserId, admin);
        entity.setStatus(EventStatus.CANCELLED);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toResponse(eventRepository.save(entity));
    }

    private EventEntity findEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void requireOwnerOrAdmin(EventEntity entity, UUID currentUserId, boolean admin) {
        if (!admin && !entity.getOrganizerId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Event access denied");
        }
    }

    private void validateTimeline(
            OffsetDateTime registrationOpenAt,
            OffsetDateTime registrationCloseAt,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        if (!endAt.isAfter(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event end time must be after start time");
        }
        if (registrationOpenAt != null && registrationCloseAt != null && registrationCloseAt.isBefore(registrationOpenAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration close must be after registration open");
        }
        if (registrationCloseAt != null && registrationCloseAt.isAfter(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration close must be before event start");
        }
    }

    private EventCategory parseCategory(String raw) {
        try {
            return EventCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event category");
        }
    }

    private EventResponse toResponse(EventEntity entity) {
        return new EventResponse(
                entity.getEventId(),
                entity.getOrganizerId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory().name(),
                entity.getLocation(),
                entity.getCapacity(),
                entity.getRegistrationOpenAt(),
                entity.getRegistrationCloseAt(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
