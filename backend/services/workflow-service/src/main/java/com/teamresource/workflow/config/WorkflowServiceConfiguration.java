package com.teamresource.workflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, InternalApiProperties.class, ClientProperties.class})
public class WorkflowServiceConfiguration {

    @Bean
    RestClient bookingRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.bookingServiceBaseUrl()).build();
    }
}
