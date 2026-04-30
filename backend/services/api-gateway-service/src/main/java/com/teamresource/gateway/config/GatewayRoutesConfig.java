package com.teamresource.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    RouteLocator gatewayRoutes(RouteLocatorBuilder builder, GatewayRouteProperties properties) {
        var services = properties.services();

        return builder.routes()
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/internal/auth/**", "/oauth2/**", "/login/oauth2/**", "/v3/api-docs/auth/**")
                        .uri(services.auth()))
                .route("user-service", r -> r
                        .path("/api/v1/users/**", "/api/v1/preferences/**", "/v3/api-docs/user/**")
                        .uri(services.user()))
                .route("event-service", r -> r
                        .path("/api/v1/events/**", "/v3/api-docs/event/**")
                        .uri(services.event()))
                .route("resource-service", r -> r
                        .path("/api/v1/resources/**", "/v3/api-docs/resource/**")
                        .uri(services.resource()))
                .route("booking-service", r -> r
                        .path("/api/v1/bookings/**", "/api/v1/booking-waitlist-entries/**", "/v3/api-docs/booking/**")
                        .uri(services.booking()))
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**", "/api/v1/notification-preferences/**", "/v3/api-docs/notification/**")
                        .uri(services.notification()))
                .route("workflow-service", r -> r
                        .path("/api/v1/workflows/**", "/api/v1/approval-tasks/**", "/v3/api-docs/workflow/**")
                        .uri(services.workflow()))
                .route("analytics-service", r -> r
                        .path("/api/v1/analytics/**", "/api/v1/dashboard/**", "/v3/api-docs/analytics/**")
                        .uri(services.analytics()))
                .build();
    }
}
