package com.teamresource.notification.application.service;

import com.teamresource.notification.common.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    public UUID userId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ApiException("AUTH_SUBJECT_MISSING", HttpStatus.UNAUTHORIZED, "Missing authenticated user");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (Exception ex) {
            throw new ApiException("AUTH_SUBJECT_INVALID", HttpStatus.UNAUTHORIZED, "Invalid authenticated user id");
        }
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a));
    }
}
