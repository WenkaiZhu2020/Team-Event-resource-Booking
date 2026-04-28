package com.teamresource.notification.infrastructure.config;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Profile("source-architecture")
public class RabbitConfig {

    @Bean
    Declarables notificationMessagingTopology(MessagingProperties properties) {
        String inboundExchangeName = properties.inboundExchange();
        String dlxName = inboundExchangeName + ".dlx";

        DirectExchange inboundExchange = new DirectExchange(inboundExchangeName, true, false);
        DirectExchange dlx = new DirectExchange(dlxName, true, false);

        Queue inboundQueue = new Queue(properties.consumers().bookingEventsQueue(), true, false, false, Map.of(
                "x-dead-letter-exchange", dlxName,
                "x-dead-letter-routing-key", properties.consumers().bookingEventsDlq()
        ));
        Queue dlqQueue = new Queue(properties.consumers().bookingEventsDlq(), true);

        Binding inboundBinding = BindingBuilder.bind(inboundQueue)
                .to(inboundExchange)
                .with(properties.inboundRoutingKey());
        Binding dlqBinding = BindingBuilder.bind(dlqQueue)
                .to(dlx)
                .with(properties.consumers().bookingEventsDlq());

        return new Declarables(inboundExchange, dlx, inboundQueue, dlqQueue, inboundBinding, dlqBinding);
    }
}
