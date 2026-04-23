package com.teamresource.auth.config;

import com.teamresource.auth.service.UserProvisioningClient;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserServiceClientConfig {

    @Bean
    UserProvisioningClient userProvisioningClient(UserServiceIntegrationProperties properties) {
        if (!properties.enabled()) {
            return (userId, email, displayName, timezone, roles) -> {
            };
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-Internal-Api-Key", properties.apiKey())
                .build();

        return (userId, email, displayName, timezone, roles) -> restClient.post()
                .uri("/api/v1/internal/users/provision")
                .body(new ProvisionUserRequest(userId, email, displayName, timezone, roles))
                .retrieve()
                .toBodilessEntity();
    }

    record ProvisionUserRequest(
            UUID userId,
            String email,
            String displayName,
            String timezone,
            Set<String> roles
    ) {
    }
}
