package com.teamresource.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest(properties = {
        "app.gateway.services.auth=http://localhost:8081",
        "app.gateway.services.user=http://localhost:8082",
        "app.gateway.services.event=http://localhost:8083",
        "app.gateway.services.resource=http://localhost:8084",
        "app.gateway.services.booking=http://localhost:8085",
        "app.gateway.services.notification=http://localhost:8086",
        "app.gateway.services.workflow=http://localhost:8087",
        "app.gateway.services.analytics=http://localhost:8088",
        "app.gateway.public-paths[0]=/api/v1/auth/**",
        "app.gateway.public-paths[1]=/oauth2/**",
        "app.security.jwt.secret-base64=VEVBTV9SRVNPVVJDRV9NQU5BR0VNRU5UX0RFVl9TRUNSRVRfSFM1Nl8zMl9CWVRFUw=="
})
class GatewayConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private OpenAPI gatewayOpenApi;

    @Test
    void shouldExposeOpenApiAndRoutes() {
        List<String> routeIds = routeLocator.getRoutes().map(route -> route.getId()).collectList().block();

        assertThat(gatewayOpenApi.getInfo().getTitle()).isEqualTo("Team Resource Management API Gateway");
        assertThat(routeIds).containsExactlyInAnyOrder(
                "auth-service",
                "user-service",
                "event-service",
                "resource-service",
                "booking-service",
                "notification-service",
                "workflow-service",
                "analytics-service"
        );
    }
}
