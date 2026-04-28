package com.teamresource.notification.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("source-architecture")
public class SchedulingConfig {
}
