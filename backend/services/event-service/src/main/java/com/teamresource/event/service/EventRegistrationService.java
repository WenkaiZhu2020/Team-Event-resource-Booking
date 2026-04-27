package com.teamresource.event.service;

import com.teamresource.event.api.dto.EventRegistrationResponse;
import com.teamresource.event.domain.EventRegistrationStatus;
import com.teamresource.event.domain.EventStatus;
import com.teamresource.event.infra.persistence.EventEntity;
import com.teamresource.event.infra.persistence.EventRegistrationEntity;
import com.teamresource.event.infra.persistence.EventRegistrationRepository;
import com.teamresource.event.infra.persistence.EventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;

    public EventRegistrationService(
            EventRepository eventRepository,
            EventRegistrationRepository eventRegistrationRepository
    ) {
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
    }

    @Transactional
    public EventRegistrationResponse register(UUID eventId, UUID userId) {
        EventEntity event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        ensureRegistrable(event);

        EventRegistrationEntity registration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId).orElse(null);
        if (registration != null && registration.getStatus() != EventRegistrationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already registered for this event");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        boolean hasCapacity = event.getAttendeeProjectedCount() < event.getCapacity();
        EventRegistrationStatus nextStatus = hasCapacity ? EventRegistrationStatus.REGISTERED : EventRegistrationStatus.WAITLISTED;

        EventRegistrationEntity entity = registration == null ? new EventRegistrationEntity() : registration;
        if (entity.getRegistrationId() == null) {
            entity.setRegistrationId(UUID.randomUUID());
            entity.setCreatedAt(now);
        }
        entity.setEventId(eventId);
        entity.setUserId(userId);
        entity.setStatus(nextStatus);
        entity.setRegisteredAt(now);
        entity.setCancelledAt(null);
        entity.setCheckedInAt(null);
        entity.setCheckedInBy(null);
        entity.setUpdatedAt(now);
        if (nextStatus == EventRegistrationStatus.WAITLISTED) {
            entity.setWaitlistPosition(nextWaitlistPosition(eventId));
            event.setWaitlistProjectedCount(event.getWaitlistProjectedCount() + 1);
        } else {
            entity.setWaitlistPosition(null);
            event.setAttendeeProjectedCount(event.getAttendeeProjectedCount() + 1);
        }
        event.setUpdatedAt(now);

        EventRegistrationEntity saved = eventRegistrationRepository.save(entity);
        eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public EventRegistrationResponse cancel(UUID eventId, UUID userId) {
        EventEntity event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        EventRegistrationEntity registration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        if (registration.getStatus() == EventRegistrationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration is already cancelled");
        }
        if (registration.getCheckedInAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Checked-in registrations cannot be cancelled");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (registration.getStatus() == EventRegistrationStatus.REGISTERED) {
            event.setAttendeeProjectedCount(Math.max(0, event.getAttendeeProjectedCount() - 1));
        } else if (registration.getStatus() == EventRegistrationStatus.WAITLISTED) {
            event.setWaitlistProjectedCount(Math.max(0, event.getWaitlistProjectedCount() - 1));
        }

        registration.setStatus(EventRegistrationStatus.CANCELLED);
        registration.setCancelledAt(now);
        registration.setUpdatedAt(now);
        registration.setWaitlistPosition(null);
        EventRegistrationEntity saved = eventRegistrationRepository.save(registration);

        if (event.getStatus() != EventStatus.CANCELLED) {
            promoteWaitlist(event, now);
        }
        event.setUpdatedAt(now);
        eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EventRegistrationResponse myRegistration(UUID eventId, UUID userId) {
        EventRegistrationEntity registration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        return toResponse(registration);
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> myRegistrations(UUID userId) {
        return eventRegistrationRepository.findByUserIdOrderByRegisteredAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> eventRegistrations(UUID eventId, UUID currentUserId, boolean admin) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (!admin && !event.getOrganizerId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Event registration access denied");
        }
        return eventRegistrationRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventRegistrationResponse checkIn(UUID eventId, UUID registrationId, UUID currentUserId, boolean admin) {
        EventEntity event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (!admin && !event.getOrganizerId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Check-in access denied");
        }

        EventRegistrationEntity registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        if (!registration.getEventId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration does not belong to the target event");
        }
        if (registration.getStatus() != EventRegistrationStatus.REGISTERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only registered attendees can be checked in");
        }
        if (registration.getCheckedInAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration is already checked in");
        }
        ensureCheckInWindow(event);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        registration.setCheckedInAt(now);
        registration.setCheckedInBy(currentUserId);
        registration.setUpdatedAt(now);
        event.setCheckedInCount(event.getCheckedInCount() + 1);
        event.setUpdatedAt(now);

        EventRegistrationEntity saved = eventRegistrationRepository.save(registration);
        eventRepository.save(event);
        return toResponse(saved);
    }

    private void ensureRegistrable(EventEntity event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only published events accept registrations");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (event.getRegistrationOpenAt() != null && now.isBefore(event.getRegistrationOpenAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration window has not opened yet");
        }
        if (event.getRegistrationCloseAt() != null && now.isAfter(event.getRegistrationCloseAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration window has already closed");
        }
        if (!now.isBefore(event.getStartAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registrations are closed after the event starts");
        }
    }

    private void ensureCheckInWindow(EventEntity event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Check-in is not available for this event");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime checkInOpenAt = event.getStartAt().minusHours(2);
        if (now.isBefore(checkInOpenAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Check-in window has not opened yet");
        }
        if (now.isAfter(event.getEndAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Check-in is closed after the event ends");
        }
    }

    private int nextWaitlistPosition(UUID eventId) {
        return eventRegistrationRepository.maxWaitlistPosition(eventId) + 1;
    }

    private void promoteWaitlist(EventEntity event, OffsetDateTime now) {
        if (event.getAttendeeProjectedCount() >= event.getCapacity()) {
            resequenceWaitlist(event.getEventId());
            return;
        }
        List<EventRegistrationEntity> waitlist = eventRegistrationRepository.findByEventIdAndStatusOrderByWaitlistPositionAsc(
                event.getEventId(),
                EventRegistrationStatus.WAITLISTED
        );
        if (waitlist.isEmpty()) {
            return;
        }
        EventRegistrationEntity promoted = waitlist.getFirst();
        promoted.setStatus(EventRegistrationStatus.REGISTERED);
        promoted.setWaitlistPosition(null);
        promoted.setUpdatedAt(now);
        eventRegistrationRepository.save(promoted);
        event.setWaitlistProjectedCount(Math.max(0, event.getWaitlistProjectedCount() - 1));
        event.setAttendeeProjectedCount(event.getAttendeeProjectedCount() + 1);
        resequenceWaitlist(event.getEventId());
    }

    private void resequenceWaitlist(UUID eventId) {
        List<EventRegistrationEntity> waitlist = eventRegistrationRepository.findByEventIdAndStatusOrderByWaitlistPositionAsc(
                eventId,
                EventRegistrationStatus.WAITLISTED
        );
        for (int index = 0; index < waitlist.size(); index++) {
            EventRegistrationEntity entity = waitlist.get(index);
            entity.setWaitlistPosition(index + 1);
            eventRegistrationRepository.save(entity);
        }
    }

    private EventRegistrationResponse toResponse(EventRegistrationEntity entity) {
        return new EventRegistrationResponse(
                entity.getRegistrationId(),
                entity.getEventId(),
                entity.getUserId(),
                entity.getStatus().name(),
                entity.getWaitlistPosition(),
                entity.getRegisteredAt(),
                entity.getCancelledAt(),
                entity.getCheckedInAt(),
                entity.getCheckedInBy(),
                entity.getUpdatedAt()
        );
    }
}
