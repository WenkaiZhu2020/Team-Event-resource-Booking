package com.teamresource.notification.api.controller;

import com.teamresource.notification.api.dto.ApiResponse;
import com.teamresource.notification.api.dto.NotificationPreferenceResponse;
import com.teamresource.notification.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.notification.application.facade.NotificationFacade;
import com.teamresource.notification.application.service.CurrentUserResolver;
import com.teamresource.notification.application.service.NotificationViewMapper;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@Profile("stage2-layered-inactive")
public class NotificationPreferenceController {

    private final NotificationFacade facade;
    private final NotificationViewMapper viewMapper;
    private final CurrentUserResolver currentUserResolver;

    public NotificationPreferenceController(
            NotificationFacade facade,
            NotificationViewMapper viewMapper,
            CurrentUserResolver currentUserResolver
    ) {
        this.facade = facade;
        this.viewMapper = viewMapper;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> myPreferences(Authentication authentication) {
        UUID userId = currentUserResolver.userId(authentication);
        NotificationPreferenceResponse response = viewMapper.toPreferenceResponse(facade.getPreference(userId));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferenceRequest request,
            Authentication authentication
    ) {
        UUID userId = currentUserResolver.userId(authentication);
        NotificationPreferenceResponse response = viewMapper.toPreferenceResponse(facade.updatePreference(userId, request));
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
