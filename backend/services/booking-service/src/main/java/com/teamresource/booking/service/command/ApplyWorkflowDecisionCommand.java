package com.teamresource.booking.service.command;

import java.util.UUID;

public record ApplyWorkflowDecisionCommand(
        UUID bookingId,
        String decision,
        String note
) {
}
