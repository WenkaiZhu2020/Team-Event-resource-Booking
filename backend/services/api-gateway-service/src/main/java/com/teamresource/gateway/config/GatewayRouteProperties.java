package com.teamresource.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gateway")
public record GatewayRouteProperties(
        Services services,
        List<String> publicPaths
) {

    public record Services(
            String auth,
            String user,
            String event,
            String resource,
            String booking,
            String notification,
            String workflow,
            String analytics
    ) {
    }
}
