package com.teamresource.workflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableRabbit
public class RabbitConfig {

    public static final String BOOKING_EVENTS_QUEUE = "workflow.booking.events";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Bean
    TopicExchange workflowEventsExchange() {
        return new TopicExchange(WorkflowServiceConfiguration.WORKFLOW_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue bookingEventsQueue() {
        return new Queue(BOOKING_EVENTS_QUEUE, true);
    }

    @Bean
    Binding bookingEventsBinding(Queue bookingEventsQueue, TopicExchange workflowEventsExchange) {
        return BindingBuilder.bind(bookingEventsQueue).to(workflowEventsExchange).with("booking.*");
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    MessagePostProcessor rabbitTraceIdMessagePostProcessor() {
        return RabbitConfig::applyTraceIdHeader;
    }

    @Bean
    static BeanPostProcessor rabbitTemplateTraceBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RabbitTemplate rabbitTemplate) {
                    rabbitTemplate.addBeforePublishPostProcessors(RabbitConfig::applyTraceIdHeader);
                }
                return bean;
            }
        };
    }

    @Bean
    MethodInterceptor rabbitListenerTraceMdcInterceptor() {
        return invocation -> {
            String previousTraceId = MDC.get(TRACE_ID_MDC_KEY);
            String headerTraceId = traceIdFromMessage(invocation.getArguments());

            if (headerTraceId != null) {
                MDC.put(TRACE_ID_MDC_KEY, headerTraceId);
            } else {
                MDC.remove(TRACE_ID_MDC_KEY);
            }

            try {
                return invocation.proceed();
            } finally {
                if (previousTraceId != null && !previousTraceId.isBlank()) {
                    MDC.put(TRACE_ID_MDC_KEY, previousTraceId);
                } else {
                    MDC.remove(TRACE_ID_MDC_KEY);
                }
            }
        };
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            MethodInterceptor rabbitListenerTraceMdcInterceptor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter);
        factory.setAdviceChain(rabbitListenerTraceMdcInterceptor);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    private static String traceIdFromMdc() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        return traceId == null || traceId.isBlank() ? null : traceId;
    }

    private static Message applyTraceIdHeader(Message message) {
        String traceId = traceIdFromMdc();
        if (traceId != null) {
            message.getMessageProperties().setHeader(TRACE_ID_HEADER, traceId);
        }
        return message;
    }

    private static String traceIdFromMessage(Object[] arguments) {
        if (arguments == null) {
            return null;
        }
        for (Object argument : arguments) {
            if (argument instanceof Message message) {
                Object headerValue = message.getMessageProperties().getHeaders().get(TRACE_ID_HEADER);
                if (headerValue instanceof String traceId && !traceId.isBlank()) {
                    return traceId;
                }
            }
        }
        return null;
    }
}
