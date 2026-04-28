package com.teamresource.event.api;

import com.teamresource.event.api.dto.ApiResponse;
import com.teamresource.event.api.dto.EventReminderCandidateResponse;
import com.teamresource.event.api.dto.EventResponse;
import com.teamresource.event.api.dto.InternalApprovalDecisionRequest;
import com.teamresource.event.service.EventRegistrationService;
import com.teamresource.event.service.EventService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/events")
public class InternalEventController {

    private final EventRegistrationService eventRegistrationService;
    private final EventService eventService;

    public InternalEventController(EventRegistrationService eventRegistrationService, EventService eventService) {
        this.eventRegistrationService = eventRegistrationService;
        this.eventService = eventService;
    }

    @GetMapping("/reminders/due")
    public ApiResponse<List<EventReminderCandidateResponse>> dueReminders(
            @RequestParam OffsetDateTime windowStart,
            @RequestParam OffsetDateTime windowEnd
    ) {
        return ApiResponse.of(eventRegistrationService.dueReminders(windowStart, windowEnd));
    }

    @PostMapping("/{eventId}/decision")
    public ApiResponse<EventResponse> applyApprovalDecision(
            @PathVariable UUID eventId,
            @Valid @RequestBody InternalApprovalDecisionRequest request
    ) {
        return ApiResponse.of(eventService.applyApprovalDecision(eventId, request));
    }
}
