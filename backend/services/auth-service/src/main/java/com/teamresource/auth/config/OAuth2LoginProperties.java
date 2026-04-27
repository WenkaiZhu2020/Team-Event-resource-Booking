package com.teamresource.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2LoginProperties(
        boolean googleEnabled,
        String successRedirectUrl,
        String failureRedirectUrl
) {
}
