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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
