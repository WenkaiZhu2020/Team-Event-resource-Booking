package com.teamresource.user.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.user.api.dto.NotificationPreferenceResponse;
import com.teamresource.user.api.dto.ProvisionUserRequest;
import com.teamresource.user.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.user.api.dto.UpdateUserProfileRequest;
import com.teamresource.user.api.dto.UserProfileResponse;
import com.teamresource.user.service.UserProfileService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserInternalController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserInternalControllerTest.TestConfig.class)
class UserInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubUserProfileService userProfileService;

    @Test
    @WithMockUser(roles = "INTERNAL_SERVICE")
    void provisionShouldReturnProvisionedProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileService.provisionResponse = new UserProfileResponse(
                userId,
                "new.user@example.com",
                "New User",
                "UTC",
                "USER",
                "ACTIVE",
                OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                OffsetDateTime.parse("2026-01-01T10:00:00Z")
        );

        mockMvc.perform(post("/api/v1/internal/users/provision")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "userId", userId,
                                "email", "new.user@example.com",
                                "displayName", "New User",
                                "timezone", "UTC",
                                "roles", java.util.List.of("USER")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("new.user@example.com"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubUserProfileService userProfileService() {
            return new StubUserProfileService();
        }
    }

    static class StubUserProfileService extends UserProfileService {

        private UserProfileResponse provisionResponse;

        StubUserProfileService() {
            super(null, null);
        }

        @Override
        public UserProfileResponse provision(ProvisionUserRequest request) {
            return provisionResponse;
        }

        @Override
        public UserProfileResponse me(UUID userId) {
            return null;
        }

        @Override
        public UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
            return null;
        }

        @Override
        public NotificationPreferenceResponse preferences(UUID userId) {
            return null;
        }

        @Override
        public NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request) {
            return null;
        }
    }
}
