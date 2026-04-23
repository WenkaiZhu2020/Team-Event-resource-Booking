package com.teamresource.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record ProvisionUserRequest(
        @NotNull UUID userId,
        @Email @NotBlank String email,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(max = 80) String timezone,
        @NotEmpty Set<String> roles
) {
}
