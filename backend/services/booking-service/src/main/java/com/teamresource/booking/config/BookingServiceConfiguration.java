package com.teamresource.booking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, ClientProperties.class})
public class BookingServiceConfiguration {

    @Bean
    RestClient resourceRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.resourceServiceBaseUrl()).build();
    }

    @Bean
    RestClient eventRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.eventServiceBaseUrl()).build();
    }
}
