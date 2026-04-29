package com.teamresource.notification.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("stage2-layered-inactive")
public class SchedulingConfig {
}
