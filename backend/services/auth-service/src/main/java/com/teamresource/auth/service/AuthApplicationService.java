package com.teamresource.auth.service;

import com.teamresource.auth.api.dto.AuthResponse;
import com.teamresource.auth.api.dto.TokenResponse;
import com.teamresource.auth.api.dto.UserResponse;
import com.teamresource.auth.domain.Role;
import com.teamresource.auth.domain.UserStatus;
import com.teamresource.auth.infra.persistence.AppUserEntity;
import com.teamresource.auth.infra.persistence.AppUserRepository;
import com.teamresource.auth.infra.security.JwtService;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthApplicationService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProvisioningClient userProvisioningClient;

    public AuthApplicationService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserProvisioningClient userProvisioningClient
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userProvisioningClient = userProvisioningClient;
    }

    @Transactional
    public AuthResponse register(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.USER));
        user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        AppUserEntity saved = userRepository.save(user);
        userProvisioningClient.provisionUser(
                saved.getId(),
                saved.getEmail(),
                saved.getEmail(),
                "UTC",
                saved.getRoles().stream().map(Role::name).collect(java.util.stream.Collectors.toSet())
        );
        return issueAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String rawPassword) {
        AppUserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        return issueAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Principal principal) {
        UUID userId = parsePrincipal(principal);
        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Role::name).collect(java.util.stream.Collectors.toSet()),
                user.getStatus().name()
        );
    }

    private AuthResponse issueAuthResponse(AppUserEntity user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        TokenResponse tokenResponse = new TokenResponse(accessToken, "Bearer", jwtService.accessTokenTtlSeconds());
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Role::name).collect(java.util.stream.Collectors.toSet()),
                user.getStatus().name()
        );
        return new AuthResponse(tokenResponse, userResponse);
    }

    private UUID parsePrincipal(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
