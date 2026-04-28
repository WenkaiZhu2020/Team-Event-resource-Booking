package com.teamresource.booking.application.client;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ResourcePrecheckGateway {

    ResourcePrecheckResult precheck(UUID resourceId, OffsetDateTime startAt, OffsetDateTime endAt);
}
