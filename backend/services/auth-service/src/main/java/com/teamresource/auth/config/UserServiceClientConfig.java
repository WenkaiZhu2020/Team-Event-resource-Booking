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
            return new UserProvisioningClient() {
                @Override
                public void provisionUser(UUID userId, String email, String displayName, String timezone, Set<String> roles) {
                }

                @Override
                public void syncRoles(UUID userId, Set<String> roles, String assignedBy) {
                }
            };
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-Internal-Api-Key", properties.apiKey())
                .build();

        return new UserProvisioningClient() {
            @Override
            public void provisionUser(UUID userId, String email, String displayName, String timezone, Set<String> roles) {
                restClient.post()
                        .uri("/api/v1/internal/users/provision")
                        .body(new ProvisionUserRequest(userId, email, displayName, timezone, roles))
                        .retrieve()
                        .toBodilessEntity();
            }

            @Override
            public void syncRoles(UUID userId, Set<String> roles, String assignedBy) {
                restClient.post()
                        .uri("/api/v1/internal/users/{userId}/roles/sync", userId)
                        .body(new SyncRolesRequest(roles, assignedBy))
                        .retrieve()
                        .toBodilessEntity();
            }
        };
    }

    record ProvisionUserRequest(
            UUID userId,
            String email,
            String displayName,
            String timezone,
            Set<String> roles
    ) {
    }

    record SyncRolesRequest(
            Set<String> roles,
            String assignedBy
    ) {
    }
}
