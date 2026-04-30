package com.teamresource.auth.service;

import com.teamresource.auth.api.dto.AuthResponse;
import com.teamresource.auth.config.JwtProperties;
import com.teamresource.auth.domain.Role;
import com.teamresource.auth.domain.UserStatus;
import com.teamresource.auth.infra.persistence.AppUserEntity;
import com.teamresource.auth.infra.persistence.AppUserRepository;
import com.teamresource.auth.infra.security.JwtService;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserProvisioningClient userProvisioningClient;

    private AuthApplicationService authApplicationService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(new JwtProperties(
                "team-resource-auth",
                "VEVBTV9SRVNPVVJDRV9NQU5BR0VNRU5UX0RFVl9TRUNSRVRfSFM1Nl8zMl9CWVRFUw==",
                60
        ));
        authApplicationService = new AuthApplicationService(
                userRepository,
                passwordEncoder,
                jwtService,
                userProvisioningClient
        );
    }

    @Test
    void registerShouldPersistNormalizedUserAndProvisionProfile() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
        when(userRepository.save(any(AppUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthResponse response = authApplicationService.register(" User@Example.com ", "Password123");

        ArgumentCaptor<AppUserEntity> userCaptor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        AppUserEntity saved = userCaptor.getValue();

        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getRoles()).containsExactly(Role.USER);

        verify(userProvisioningClient).provisionUser(
                saved.getId(),
                "user@example.com",
                "user@example.com",
                "UTC",
                Set.of("USER")
        );

        assertThat(response.tokens().accessToken()).isNotBlank();
        assertThat(response.tokens().expiresInSeconds()).isEqualTo(3600L);
        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().roles()).containsExactly("USER");
    }

    @Test
    void updateRolesShouldPersistAndSyncToUserService() {
        UUID userId = UUID.randomUUID();
        AppUserEntity user = new AppUserEntity();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.USER));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(AppUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authApplicationService.updateRoles(userId, Set.of("admin", "user"));

        assertThat(response.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
        verify(userProvisioningClient).syncRoles(userId, Set.of("ADMIN", "USER"), "auth-service");
    }

    @Test
    void updateRolesShouldRejectInvalidRole() {
        UUID userId = UUID.randomUUID();
        AppUserEntity user = new AppUserEntity();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.USER));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authApplicationService.updateRoles(userId, Set.of("missing")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any(AppUserEntity.class));
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.USER));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authApplicationService.login("user@example.com", "wrong-password"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void meShouldRejectInvalidPrincipal() {
        Principal principal = () -> "not-a-uuid";

        assertThatThrownBy(() -> authApplicationService.me(principal))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authApplicationService.register("user@example.com", "Password123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any(AppUserEntity.class));
    }

    @Test
    void loginShouldReturnAuthResponseForActiveUser() {
        AppUserEntity user = activeUser("user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("Password123", "encoded-password")).thenReturn(true);

        AuthResponse response = authApplicationService.login(" user@example.com ", "Password123");

        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().roles()).containsExactly("USER");
        assertThat(response.tokens().accessToken()).isNotBlank();
    }

    @Test
    void loginShouldRejectMissingUser() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authApplicationService.login("user@example.com", "Password123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginShouldRejectInactiveUser() {
        AppUserEntity user = activeUser("user@example.com");
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("Password123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authApplicationService.login("user@example.com", "Password123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void googleLoginShouldProvisionNewUserWithEmailFallbackWhenDisplayNameMissing() {
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("oauth-password");
        when(userRepository.save(any(AppUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authApplicationService.loginOrRegisterGoogle(
                " User@example.com ",
                "google-subject",
                null,
                "https://cdn.example/avatar.png",
                true
        );

        ArgumentCaptor<AppUserEntity> userCaptor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        AppUserEntity saved = userCaptor.getValue();

        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getGoogleSubject()).isEqualTo("google-subject");
        assertThat(saved.getDisplayName()).isNull();
        verify(userProvisioningClient).provisionUser(
                saved.getId(),
                "user@example.com",
                "user@example.com",
                "UTC",
                Set.of("USER")
        );
        assertThat(response.user().email()).isEqualTo("user@example.com");
    }

    @Test
    void googleLoginShouldProvisionNewUserWithProvidedDisplayName() {
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("oauth-password");
        when(userRepository.save(any(AppUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authApplicationService.loginOrRegisterGoogle(
                "user@example.com",
                "google-subject",
                "Google User",
                "https://cdn.example/avatar.png",
                true
        );

        ArgumentCaptor<AppUserEntity> userCaptor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        AppUserEntity saved = userCaptor.getValue();

        verify(userProvisioningClient).provisionUser(
                saved.getId(),
                "user@example.com",
                "Google User",
                "UTC",
                Set.of("USER")
        );
    }

    @Test
    void googleLoginShouldUpdateExistingUserWithoutProvisioningAgain() {
        AppUserEntity user = activeUser("user@example.com");
        user.setUpdatedAt(OffsetDateTime.now());
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(AppUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authApplicationService.loginOrRegisterGoogle(
                "user@example.com",
                "google-subject",
                "Google User",
                "",
                false
        );

        verify(userProvisioningClient, never()).provisionUser(any(), any(), any(), any(), any());
        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Google User");
        assertThat(user.getAvatarUrl()).isEmpty();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void googleLoginShouldRejectInactiveUser() {
        AppUserEntity user = activeUser("user@example.com");
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authApplicationService.loginOrRegisterGoogle(
                "user@example.com",
                "google-subject",
                "Google User",
                null,
                true
        )).isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void meShouldReturnCurrentUser() {
        UUID userId = UUID.randomUUID();
        AppUserEntity user = activeUser("viewer@example.com");
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        var response = authApplicationService.me(userId::toString);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("viewer@example.com");
    }

    @Test
    void meShouldRejectUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authApplicationService.me(userId::toString))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateRolesShouldRejectMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authApplicationService.updateRoles(userId, Set.of("USER")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateRolesShouldRejectEmptyRolesAfterNormalization() {
        UUID userId = UUID.randomUUID();
        AppUserEntity user = activeUser("user@example.com");
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authApplicationService.updateRoles(userId, Set.of("  ")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateRolesShouldRejectEmptyRoleSet() {
        UUID userId = UUID.randomUUID();
        AppUserEntity user = activeUser("user@example.com");
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authApplicationService.updateRoles(userId, Collections.emptySet()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void smokeShouldInstantiateService() {
        assertThatCode(() -> new AuthApplicationService(userRepository, passwordEncoder, new JwtService(new JwtProperties(
                "issuer",
                "VEVBTV9SRVNPVVJDRV9NQU5BR0VNRU5UX0RFVl9TRUNSRVRfSFM1Nl8zMl9CWVRFUw==",
                60
        )), userProvisioningClient)).doesNotThrowAnyException();
    }

    private AppUserEntity activeUser(String email) {
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.USER));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }
}
