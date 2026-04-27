package com.teamresource.event.infra.persistence;

import com.teamresource.event.domain.EventRegistrationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRegistrationRepository extends JpaRepository<EventRegistrationEntity, UUID> {

    Optional<EventRegistrationEntity> findByEventIdAndUserId(UUID eventId, UUID userId);

    List<EventRegistrationEntity> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    List<EventRegistrationEntity> findByUserIdOrderByRegisteredAtDesc(UUID userId);

    List<EventRegistrationEntity> findByEventIdAndStatusOrderByWaitlistPositionAsc(UUID eventId, EventRegistrationStatus status);

    @Query("select coalesce(max(r.waitlistPosition), 0) from EventRegistrationEntity r where r.eventId = :eventId and r.status = 'WAITLISTED'")
    Integer maxWaitlistPosition(UUID eventId);
}
