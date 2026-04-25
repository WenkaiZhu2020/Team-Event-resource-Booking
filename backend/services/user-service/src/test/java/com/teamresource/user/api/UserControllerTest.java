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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerTest.TestConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubUserProfileService userProfileService;

    @Test
    void meShouldReturnProfilePayload() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileService.meResponse = new UserProfileResponse(
                userId,
                "member@example.com",
                "Member",
                "UTC",
                "USER",
                "ACTIVE",
                OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                OffsetDateTime.parse("2026-01-02T10:00:00Z")
        );

        mockMvc.perform(get("/api/v1/users/me").principal(() -> userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("member@example.com"))
                .andExpect(jsonPath("$.data.displayName").value("Member"));
    }

    @Test
    void updateNotificationPreferencesShouldValidateBounds() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/preferences/notifications")
                        .principal(() -> userId.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "inAppEnabled", true,
                                "emailEnabled", true,
                                "reminderMinutesBefore", 20000
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void updateProfileShouldReturnUpdatedPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        userProfileService.updateProfileResponse = new UserProfileResponse(
                userId,
                "member@example.com",
                "Updated Member",
                "Europe/Dublin",
                "USER",
                "ACTIVE",
                OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                OffsetDateTime.parse("2026-01-03T10:00:00Z")
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .principal(() -> userId.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "displayName", "Updated Member",
                                "timezone", "Europe/Dublin"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Updated Member"))
                .andExpect(jsonPath("$.data.timezone").value("Europe/Dublin"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubUserProfileService userProfileService() {
            return new StubUserProfileService();
        }
    }

    static class StubUserProfileService extends UserProfileService {

        private UserProfileResponse meResponse;
        private UserProfileResponse updateProfileResponse;

        StubUserProfileService() {
            super(null, null);
        }

        @Override
        public UserProfileResponse me(UUID userId) {
            return meResponse;
        }

        @Override
        public UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
            return updateProfileResponse;
        }

        @Override
        public NotificationPreferenceResponse preferences(UUID userId) {
            return null;
        }

        @Override
        public NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request) {
            return null;
        }

        @Override
        public UserProfileResponse provision(ProvisionUserRequest request) {
            return null;
        }
    }
}
