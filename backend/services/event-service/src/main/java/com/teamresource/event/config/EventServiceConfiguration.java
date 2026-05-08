package com.teamresource.event.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({ClientProperties.class, EventApprovalProperties.class})
public class EventServiceConfiguration {

    @Bean
    RestClient workflowRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.workflowServiceBaseUrl()).build();
    }
}
