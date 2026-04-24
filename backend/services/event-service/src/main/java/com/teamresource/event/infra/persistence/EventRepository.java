package com.teamresource.event.infra.persistence;

import com.teamresource.event.domain.EventStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {
    List<EventEntity> findByStatusOrderByStartAtAsc(EventStatus status);
    List<EventEntity> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);
    Optional<EventEntity> findByEventIdAndOrganizerId(UUID eventId, UUID organizerId);
}
