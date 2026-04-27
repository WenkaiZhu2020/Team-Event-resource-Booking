package com.teamresource.analytics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {

    public static final String DOMAIN_EVENTS_EXCHANGE = "team-resource.events";

    @Bean
    TopicExchange domainEventsExchange() {
        return new TopicExchange(DOMAIN_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange analyticsDeadLetterExchange() {
        return new DirectExchange("analytics.dead-letter", true, false);
    }

    @Bean
    Queue analyticsQueue(AnalyticsProperties properties) {
        return new Queue(properties.queueName(), true, false, false, java.util.Map.of(
                "x-dead-letter-exchange", "analytics.dead-letter",
                "x-dead-letter-routing-key", properties.deadLetterQueueName()
        ));
    }

    @Bean
    Queue analyticsDeadLetterQueue(AnalyticsProperties properties) {
        return new Queue(properties.deadLetterQueueName(), true);
    }

    @Bean
    Binding analyticsBookingBinding(Queue analyticsQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(analyticsQueue).to(domainEventsExchange).with("booking.*");
    }

    @Bean
    Binding analyticsWorkflowBinding(Queue analyticsQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(analyticsQueue).to(domainEventsExchange).with("workflow.*");
    }

    @Bean
    Binding analyticsDeadLetterBinding(Queue analyticsDeadLetterQueue, DirectExchange analyticsDeadLetterExchange, AnalyticsProperties properties) {
        return BindingBuilder.bind(analyticsDeadLetterQueue).to(analyticsDeadLetterExchange).with(properties.deadLetterQueueName());
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
