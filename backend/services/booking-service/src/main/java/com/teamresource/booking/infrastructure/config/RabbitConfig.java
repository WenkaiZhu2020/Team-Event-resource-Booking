package com.teamresource.booking.infrastructure.config;

import org.springframework.context.annotation.Profile;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Profile("stage2-layered-inactive")
public class RabbitConfig {

    @Bean
    Declarables bookingMessagingTopology(MessagingProperties properties) {
        DirectExchange outboundExchange = new DirectExchange(properties.exchange(), true, false);

        Queue inboundQueue = new Queue(properties.consumers().eventResourceQueue(), true);
        DirectExchange inboundExchange = new DirectExchange("platform.domain.exchange", true, false);
        Binding binding = BindingBuilder.bind(inboundQueue)
                .to(inboundExchange)
                .with("domain.event");

        return new Declarables(outboundExchange, inboundExchange, inboundQueue, binding);
    }
}
