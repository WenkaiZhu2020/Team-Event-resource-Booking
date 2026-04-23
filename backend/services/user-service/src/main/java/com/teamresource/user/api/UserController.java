package com.teamresource.user.api;

import com.teamresource.user.api.dto.ApiResponse;
import com.teamresource.user.api.dto.NotificationPreferenceResponse;
import com.teamresource.user.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.user.api.dto.UpdateUserProfileRequest;
import com.teamresource.user.api.dto.UserProfileResponse;
import com.teamresource.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/users/me")
    public ApiResponse<UserProfileResponse> me(Principal principal) {
        return ApiResponse.of(userProfileService.me(parsePrincipal(principal)));
    }

    @PutMapping("/users/me")
    public ApiResponse<UserProfileResponse> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.of(userProfileService.updateProfile(parsePrincipal(principal), request));
    }

    @GetMapping("/preferences/notifications")
    public ApiResponse<NotificationPreferenceResponse> notificationPreferences(Principal principal) {
        return ApiResponse.of(userProfileService.preferences(parsePrincipal(principal)));
    }

    @PutMapping("/preferences/notifications")
    public ApiResponse<NotificationPreferenceResponse> updateNotificationPreferences(
            Principal principal,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        return ApiResponse.of(userProfileService.updatePreferences(parsePrincipal(principal), request));
    }

    private UUID parsePrincipal(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }
}
