package com.teamresource.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, InternalApiProperties.class})
public class UserServiceConfiguration {
}
