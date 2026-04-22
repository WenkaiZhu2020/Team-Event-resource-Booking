package com.teamresource.auth.api.dto;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String email,
        Set<String> roles,
        String status
) {
}
