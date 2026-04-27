package com.teamresource.event.infra.persistence;

import com.teamresource.event.domain.EventStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {
    List<EventEntity> findByStatusOrderByStartAtAsc(EventStatus status);
    List<EventEntity> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);
    Optional<EventEntity> findByEventIdAndOrganizerId(UUID eventId, UUID organizerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventEntity e where e.eventId = :eventId")
    Optional<EventEntity> findByIdForUpdate(UUID eventId);
}
