package com.teamresource.event.api;

import com.teamresource.event.api.dto.ApiResponse;
import com.teamresource.event.api.dto.CreateEventRequest;
import com.teamresource.event.api.dto.EventResponse;
import com.teamresource.event.api.dto.UpdateEventRequest;
import com.teamresource.event.service.EventService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EventResponse> create(Principal principal, @Valid @RequestBody CreateEventRequest request) {
        return ApiResponse.of(eventService.create(parsePrincipal(principal), request));
    }

    @PutMapping("/{eventId}")
    public ApiResponse<EventResponse> update(
            @PathVariable UUID eventId,
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        return ApiResponse.of(eventService.update(eventId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN"), request));
    }

    @GetMapping
    public ApiResponse<List<EventResponse>> publishedEvents() {
        return ApiResponse.of(eventService.publishedEvents());
    }

    @GetMapping("/me")
    public ApiResponse<List<EventResponse>> myEvents(Principal principal) {
        return ApiResponse.of(eventService.myEvents(parsePrincipal(principal)));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> byId(@PathVariable UUID eventId) {
        return ApiResponse.of(eventService.byId(eventId));
    }

    @PostMapping("/{eventId}/publish")
    public ApiResponse<EventResponse> publish(@PathVariable UUID eventId, Principal principal, Authentication authentication) {
        return ApiResponse.of(eventService.publish(eventId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    @PostMapping("/{eventId}/cancel")
    public ApiResponse<EventResponse> cancel(@PathVariable UUID eventId, Principal principal, Authentication authentication) {
        return ApiResponse.of(eventService.cancel(eventId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    private UUID parsePrincipal(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
