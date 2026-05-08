package com.teamresource.workflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ClientProperties.class)
public class WorkflowServiceConfiguration {

    public static final String WORKFLOW_EVENTS_EXCHANGE = "team-resource.events";

    @Bean
    RestClient bookingRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.bookingServiceBaseUrl()).build();
    }

    @Bean
    RestClient eventRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.eventServiceBaseUrl()).build();
    }

    @Bean
    RestClient resourceRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.resourceServiceBaseUrl()).build();
    }
}
