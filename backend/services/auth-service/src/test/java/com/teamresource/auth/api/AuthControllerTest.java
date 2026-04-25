package com.teamresource.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.auth.api.dto.AuthResponse;
import com.teamresource.auth.api.dto.TokenResponse;
import com.teamresource.auth.api.dto.UserResponse;
import com.teamresource.auth.service.AuthApplicationService;
import java.security.Principal;
import java.util.Set;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthControllerTest.TestConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubAuthApplicationService authApplicationService;

    @Test
    void registerShouldReturnCreatedResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        authApplicationService.registerResponse = new AuthResponse(
                new TokenResponse("jwt-token", "Bearer", 3600),
                new UserResponse(userId, "user@example.com", Set.of("USER"), "ACTIVE")
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "user@example.com",
                                "password", "Password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tokens.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
    }

    @Test
    void loginShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "bad-email",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void meShouldReturnCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        authApplicationService.meResponse = new UserResponse(
                userId,
                "viewer@example.com",
                Set.of("USER"),
                "ACTIVE"
        );

        mockMvc.perform(get("/api/v1/auth/me").principal(() -> userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("viewer@example.com"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubAuthApplicationService authApplicationService() {
            return new StubAuthApplicationService();
        }
    }

    static class StubAuthApplicationService extends AuthApplicationService {

        private AuthResponse registerResponse;
        private AuthResponse loginResponse;
        private UserResponse meResponse;

        StubAuthApplicationService() {
            super(null, null, null, null);
        }

        @Override
        public AuthResponse register(String email, String rawPassword) {
            return registerResponse;
        }

        @Override
        public AuthResponse login(String email, String rawPassword) {
            return loginResponse;
        }

        @Override
        public UserResponse me(Principal principal) {
            return meResponse;
        }
    }
}
