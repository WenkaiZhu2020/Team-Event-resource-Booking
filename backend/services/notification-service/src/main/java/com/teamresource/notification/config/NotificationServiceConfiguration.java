package com.teamresource.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({ClientProperties.class, ReminderProperties.class})
public class NotificationServiceConfiguration {

    @Bean
    RestClient eventRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.eventServiceBaseUrl()).build();
    }
}
