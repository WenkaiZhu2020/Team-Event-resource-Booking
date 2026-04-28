package com.teamresource.event.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.approval")
public record EventApprovalProperties(
        int adminCapacityThreshold
) {
}
