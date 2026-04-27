package com.teamresource.auth.infra.security;

import com.teamresource.auth.api.dto.AuthResponse;
import com.teamresource.auth.config.OAuth2LoginProperties;
import com.teamresource.auth.service.AuthApplicationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthApplicationService authApplicationService;
    private final OAuth2LoginProperties properties;

    public OAuth2AuthenticationSuccessHandler(
            AuthApplicationService authApplicationService,
            OAuth2LoginProperties properties
    ) {
        this.authApplicationService = authApplicationService;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = attribute(oauth2User, "email");
        String subject = attribute(oauth2User, "sub");
        String displayName = attributeOrDefault(oauth2User, "name", email);
        String avatarUrl = attributeOrDefault(oauth2User, "picture", null);
        boolean emailVerified = Boolean.TRUE.equals(oauth2User.getAttribute("email_verified"));

        AuthResponse authResponse = authApplicationService.loginOrRegisterGoogle(
                email,
                subject,
                displayName,
                avatarUrl,
                emailVerified
        );

        String redirectUrl = UriComponentsBuilder.fromUriString(properties.successRedirectUrl())
                .queryParam("accessToken", authResponse.tokens().accessToken())
                .queryParam("tokenType", authResponse.tokens().tokenType())
                .queryParam("expiresIn", authResponse.tokens().expiresInSeconds())
                .queryParam("userId", authResponse.user().userId())
                .queryParam("email", authResponse.user().email())
                .queryParam("roles", join(authResponse.user().roles()))
                .build(true)
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private String attribute(OAuth2User oauth2User, String key) {
        String value = oauth2User.getAttribute(key);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing OAuth2 attribute: " + key);
        }
        return value;
    }

    private String attributeOrDefault(OAuth2User oauth2User, String key, String fallback) {
        String value = oauth2User.getAttribute(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String join(Collection<String> values) {
        return values.stream().sorted().collect(Collectors.joining(","));
    }
}
