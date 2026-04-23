package com.teamresource.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(max = 80) String timezone
) {
}
