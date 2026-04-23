package com.teamresource.user.api;

import com.teamresource.user.api.dto.ApiResponse;
import com.teamresource.user.api.dto.ProvisionUserRequest;
import com.teamresource.user.api.dto.UserProfileResponse;
import com.teamresource.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/users")
public class UserInternalController {

    private final UserProfileService userProfileService;

    public UserInternalController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping("/provision")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ApiResponse<UserProfileResponse> provision(@Valid @RequestBody ProvisionUserRequest request) {
        return ApiResponse.of(userProfileService.provision(request));
    }
}
