package com.teamresource.event.infra.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface EventReminderCandidateProjection {

    UUID getRegistrationId();

    UUID getEventId();

    UUID getUserId();

    String getEventTitle();

    String getLocation();

    OffsetDateTime getStartAt();
}
