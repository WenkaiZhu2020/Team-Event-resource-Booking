package com.teamresource.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamresource.gateway.config.GatewayRouteProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class ApiGatewayCoverageTest {

    @Test
    void shouldCoverApplicationMainAndProperties() {
        GatewayRouteProperties properties = new GatewayRouteProperties(
                new GatewayRouteProperties.Services(
                        "http://auth",
                        "http://user",
                        "http://event",
                        "http://resource",
                        "http://booking",
                        "http://notification",
                        "http://workflow",
                        "http://analytics"
                ),
                List.of("/api/v1/auth/**")
        );

        assertThat(properties.services().workflow()).isEqualTo("http://workflow");
        assertThat(properties.publicPaths()).containsExactly("/api/v1/auth/**");

        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            ApiGatewayApplication.main(new String[]{"--spring.main.banner-mode=off"});
            springApplication.verify(() -> SpringApplication.run(ApiGatewayApplication.class, new String[]{"--spring.main.banner-mode=off"}));
        }
    }
}
