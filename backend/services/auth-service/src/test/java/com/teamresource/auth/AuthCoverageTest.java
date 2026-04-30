package com.teamresource.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamresource.auth.api.dto.ApiResponse;
import com.teamresource.auth.api.dto.AuthResponse;
import com.teamresource.auth.api.dto.LoginRequest;
import com.teamresource.auth.api.dto.RegisterRequest;
import com.teamresource.auth.api.dto.TokenResponse;
import com.teamresource.auth.api.dto.UpdateRolesRequest;
import com.teamresource.auth.api.dto.UserResponse;
import com.teamresource.auth.domain.Role;
import com.teamresource.auth.domain.UserStatus;
import com.teamresource.auth.infra.persistence.AppUserEntity;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class AuthCoverageTest {

    @Test
    void shouldCoverDtosEnumsAndEntity() {
        UUID userId = UUID.randomUUID();
        ApiResponse<String> apiResponse = ApiResponse.of("ok");
        LoginRequest loginRequest = new LoginRequest("user@example.com", "Password123");
        RegisterRequest registerRequest = new RegisterRequest("user@example.com", "Password123");
        TokenResponse tokenResponse = new TokenResponse("token", "Bearer", 3600);
        UserResponse userResponse = new UserResponse(userId, "user@example.com", Set.of("USER"), "ACTIVE");
        AuthResponse authResponse = new AuthResponse(tokenResponse, userResponse);
        UpdateRolesRequest updateRolesRequest = new UpdateRolesRequest(Set.of("ADMIN"));

        AppUserEntity entity = new AppUserEntity();
        entity.setId(userId);
        entity.setEmail("user@example.com");
        entity.setPasswordHash("encoded");
        entity.setDisplayName("User");
        entity.setAvatarUrl("avatar");
        entity.setEmailVerified(true);
        entity.setGoogleSubject("sub");
        entity.setGoogleLinkedAt(OffsetDateTime.now());
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setVersion(1L);
        entity.setRoles(Set.of(Role.ADMIN, Role.USER));

        assertThat(apiResponse.data()).isEqualTo("ok");
        assertThat(loginRequest.email()).isEqualTo("user@example.com");
        assertThat(registerRequest.password()).isEqualTo("Password123");
        assertThat(tokenResponse.accessToken()).isEqualTo("token");
        assertThat(userResponse.roles()).containsExactly("USER");
        assertThat(authResponse.user().email()).isEqualTo("user@example.com");
        assertThat(updateRolesRequest.roles()).containsExactly("ADMIN");
        assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
        assertThat(UserStatus.valueOf("ACTIVE")).isEqualTo(UserStatus.ACTIVE);
        assertThat(entity.getId()).isEqualTo(userId);
        assertThat(entity.getEmail()).isEqualTo("user@example.com");
        assertThat(entity.getPasswordHash()).isEqualTo("encoded");
        assertThat(entity.getDisplayName()).isEqualTo("User");
        assertThat(entity.getAvatarUrl()).isEqualTo("avatar");
        assertThat(entity.isEmailVerified()).isTrue();
        assertThat(entity.getGoogleSubject()).isEqualTo("sub");
        assertThat(entity.getGoogleLinkedAt()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getVersion()).isEqualTo(1L);
        assertThat(entity.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.USER);
    }

    @Test
    void mainShouldDelegateToSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            AuthServiceApplication.main(new String[]{"--spring.main.banner-mode=off"});

            springApplication.verify(() -> SpringApplication.run(AuthServiceApplication.class, new String[]{"--spring.main.banner-mode=off"}));
        }
    }
}
