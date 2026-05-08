package com.teamresource.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.teamresource.auth.service.UserProvisioningClient;
import com.teamresource.common.security.JwtProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthConfigAndClientTest {

    @Test
    void shouldExposeConfigurationRecordsAndPasswordEncoder() {
        JwtProperties jwtProperties = new JwtProperties("issuer", "secret", 60L);
        UserServiceIntegrationProperties integrationProperties = new UserServiceIntegrationProperties(true, "http://localhost", "key");
        OAuth2LoginProperties oAuth2LoginProperties = new OAuth2LoginProperties(true, "http://success", "http://failure");
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

        assertThat(jwtProperties.issuer()).isEqualTo("issuer");
        assertThat(integrationProperties.apiKey()).isEqualTo("key");
        assertThat(oAuth2LoginProperties.googleEnabled()).isTrue();
        assertThat(encoder.matches("Password123", encoder.encode("Password123"))).isTrue();
        assertThat(new AuthConfiguration()).isNotNull();
    }

    @Test
    void disabledClientShouldNoOp() {
        UserServiceClientConfig config = new UserServiceClientConfig();
        UserProvisioningClient client = config.userProvisioningClient(
                new UserServiceIntegrationProperties(false, "http://localhost", "key"));

        client.provisionUser(UUID.randomUUID(), "user@example.com", "User", "UTC", Set.of("USER"));
        client.syncRoles(UUID.randomUUID(), Set.of("ADMIN"), "auth-service");
    }

    @Test
    void enabledClientShouldCallProvisionAndSyncEndpoints() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> firstPath = new AtomicReference<>();
        AtomicReference<String> secondPath = new AtomicReference<>();
        AtomicReference<String> firstBody = new AtomicReference<>();
        AtomicReference<String> secondBody = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();

        server.createContext("/", exchange -> {
            int index = counter.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (index == 1) {
                firstPath.set(exchange.getRequestURI().getPath());
                firstBody.set(body);
            } else {
                secondPath.set(exchange.getRequestURI().getPath());
                secondBody.set(body);
            }
            respond(exchange);
        });
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            UserProvisioningClient client = new UserServiceClientConfig().userProvisioningClient(
                    new UserServiceIntegrationProperties(true, baseUrl, "secret"));
            UUID userId = UUID.randomUUID();

            client.provisionUser(userId, "user@example.com", "User", "UTC", Set.of("USER"));
            client.syncRoles(userId, Set.of("ADMIN", "USER"), "auth-service");

            assertThat(firstPath.get()).isEqualTo("/api/v1/internal/users/provision");
            assertThat(firstBody.get()).contains("user@example.com");
            assertThat(secondPath.get()).isEqualTo("/api/v1/internal/users/" + userId + "/roles/sync");
            assertThat(secondBody.get()).contains("auth-service");
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(new byte[0]);
        }
    }
}
