package com.teamresource.auth.api.dto;

public record AuthResponse(
        TokenResponse tokens,
        UserResponse user
) {
}
