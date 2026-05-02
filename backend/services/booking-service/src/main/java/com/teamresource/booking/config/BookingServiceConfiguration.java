package com.teamresource.booking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableRabbit
@EnableConfigurationProperties({JwtProperties.class, InternalApiProperties.class, ClientProperties.class})
public class BookingServiceConfiguration {

    public static final String BOOKING_EVENTS_EXCHANGE = "team-resource.events";

    @Bean
    RestClient resourceRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.resourceServiceBaseUrl()).build();
    }

    @Bean
    RestClient eventRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.eventServiceBaseUrl()).build();
    }

    @Bean
    RestClient workflowRestClient(ClientProperties properties) {
        return RestClient.builder().baseUrl(properties.workflowServiceBaseUrl()).build();
    }

    @Bean
    TopicExchange bookingEventsExchange() {
        return new TopicExchange(BOOKING_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue bookingSagaCompensationQueue(
            @Value("${app.saga.compensation-queue:booking.saga.compensation}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding workflowApprovalRejectedBinding(
            Queue bookingSagaCompensationQueue,
            TopicExchange bookingEventsExchange
    ) {
        return BindingBuilder.bind(bookingSagaCompensationQueue)
                .to(bookingEventsExchange)
                .with("workflow.approval.rejected");
    }

    @Bean
    Binding resourceAllocationFailedBinding(
            Queue bookingSagaCompensationQueue,
            TopicExchange bookingEventsExchange
    ) {
        return BindingBuilder.bind(bookingSagaCompensationQueue)
                .to(bookingEventsExchange)
                .with("resource.allocation.failed");
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter);
        return factory;
    }
}
