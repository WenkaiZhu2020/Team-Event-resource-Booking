package com.teamresource.notification.api;

import com.teamresource.notification.api.dto.ApiResponse;
import com.teamresource.notification.api.dto.NotificationPreferenceResponse;
import com.teamresource.notification.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.notification.application.service.CurrentUserResolver;
import com.teamresource.notification.application.service.NotificationViewMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private final com.teamresource.notification.application.facade.NotificationFacade enhancedFacade;
    private final NotificationViewMapper notificationViewMapper;
    private final CurrentUserResolver currentUserResolver;

    public NotificationPreferenceController(
            com.teamresource.notification.application.facade.NotificationFacade enhancedFacade,
            NotificationViewMapper notificationViewMapper,
            CurrentUserResolver currentUserResolver
    ) {
        this.enhancedFacade = enhancedFacade;
        this.notificationViewMapper = notificationViewMapper;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/me")
    public ApiResponse<NotificationPreferenceResponse> myPreferences(Authentication authentication) {
        UUID userId = requireUser(authentication);
        return ApiResponse.of(notificationViewMapper.toPreferenceResponse(enhancedFacade.getPreference(userId)));
    }

    @PutMapping("/me")
    public ApiResponse<NotificationPreferenceResponse> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferenceRequest request,
            Authentication authentication
    ) {
        UUID userId = requireUser(authentication);
        return ApiResponse.of(notificationViewMapper.toPreferenceResponse(enhancedFacade.updatePreference(userId, request)));
    }

    private UUID requireUser(Authentication authentication) {
        try {
            return currentUserResolver.userId(authentication);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }
}
