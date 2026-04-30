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
import java.util.stream.Collectors;
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
        user.setDisplayName(normalizedEmail);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.USER));
        user.setEmailVerified(false);
        user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        AppUserEntity saved = userRepository.save(user);
        userProvisioningClient.provisionUser(
                saved.getId(),
                saved.getEmail(),
                saved.getEmail(),
                "UTC",
                saved.getRoles().stream().map(Role::name).collect(Collectors.toSet())
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

    @Transactional
    public AuthResponse loginOrRegisterGoogle(
            String email,
            String googleSubject,
            String displayName,
            String avatarUrl,
            boolean emailVerified
    ) {
        String normalizedEmail = normalizeEmail(email);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AppUserEntity user = userRepository.findByGoogleSubject(googleSubject)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .orElseGet(() -> {
                    AppUserEntity entity = new AppUserEntity();
                    entity.setId(UUID.randomUUID());
                    entity.setEmail(normalizedEmail);
                    entity.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    entity.setRoles(Set.of(Role.USER));
                    entity.setStatus(UserStatus.ACTIVE);
                    entity.setCreatedAt(now);
                    return entity;
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        user.setEmail(normalizedEmail);
        boolean newUser = user.getUpdatedAt() == null;
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.setEmailVerified(emailVerified);
        user.setGoogleSubject(googleSubject);
        user.setGoogleLinkedAt(now);
        user.setUpdatedAt(now);

        AppUserEntity saved = userRepository.save(user);
        if (newUser) {
            userProvisioningClient.provisionUser(
                    saved.getId(),
                    saved.getEmail(),
                    saved.getDisplayName() == null ? saved.getEmail() : saved.getDisplayName(),
                    "UTC",
                    saved.getRoles().stream().map(Role::name).collect(Collectors.toSet())
            );
        }
        return issueAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Principal principal) {
        UUID userId = parsePrincipal(principal);
        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet()),
                user.getStatus().name()
        );
    }

    @Transactional
    public UserResponse updateRoles(UUID userId, Set<String> roleNames) {
        AppUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Set<Role> roles;
        try {
            roles = roleNames.stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more roles are invalid");
        }

        if (roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required");
        }

        user.setRoles(roles);
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        AppUserEntity saved = userRepository.save(user);
        userProvisioningClient.syncRoles(saved.getId(), roles.stream().map(Role::name).collect(Collectors.toSet()), "auth-service");
        return new UserResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getRoles().stream().map(Role::name).collect(Collectors.toSet()),
                saved.getStatus().name()
        );
    }

    private AuthResponse issueAuthResponse(AppUserEntity user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        TokenResponse tokenResponse = new TokenResponse(accessToken, "Bearer", jwtService.accessTokenTtlSeconds());
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet()),
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
