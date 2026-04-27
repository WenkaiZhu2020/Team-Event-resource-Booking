package com.teamresource.event.api;

import com.teamresource.event.api.dto.ApiResponse;
import com.teamresource.event.api.dto.EventReminderCandidateResponse;
import com.teamresource.event.service.EventRegistrationService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/events")
public class InternalEventController {

    private final EventRegistrationService eventRegistrationService;

    public InternalEventController(EventRegistrationService eventRegistrationService) {
        this.eventRegistrationService = eventRegistrationService;
    }

    @GetMapping("/reminders/due")
    public ApiResponse<List<EventReminderCandidateResponse>> dueReminders(
            @RequestParam OffsetDateTime windowStart,
            @RequestParam OffsetDateTime windowEnd
    ) {
        return ApiResponse.of(eventRegistrationService.dueReminders(windowStart, windowEnd));
    }
}
