package com.teamresource.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({UserServiceIntegrationProperties.class, OAuth2LoginProperties.class})
public class AuthConfiguration {
}
