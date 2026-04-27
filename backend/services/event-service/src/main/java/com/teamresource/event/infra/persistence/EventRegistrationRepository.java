package com.teamresource.event.infra.persistence;

import com.teamresource.event.domain.EventRegistrationStatus;
import java.time.OffsetDateTime;
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

    @Query("""
            select
                r.registrationId as registrationId,
                r.eventId as eventId,
                r.userId as userId,
                e.title as eventTitle,
                e.location as location,
                e.startAt as startAt
            from EventRegistrationEntity r, EventEntity e
            where e.eventId = r.eventId
              and r.status = com.teamresource.event.domain.EventRegistrationStatus.REGISTERED
              and r.checkedInAt is null
              and e.status = com.teamresource.event.domain.EventStatus.PUBLISHED
              and e.startAt > :windowStart
              and e.startAt <= :windowEnd
            order by e.startAt asc
            """)
    List<EventReminderCandidateProjection> findDueReminderCandidates(OffsetDateTime windowStart, OffsetDateTime windowEnd);
}
