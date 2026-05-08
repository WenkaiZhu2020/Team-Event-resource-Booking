package com.teamresource.notification.config;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTracePropagationTest {

    @Test
    void publisherShouldCopyTraceIdFromMdcIntoRabbitHeader() {
        RabbitConfig config = new RabbitConfig();
        MessagePostProcessor processor = config.rabbitTraceIdMessagePostProcessor();
        Message message = MessageBuilder.withBody("payload".getBytes()).build();

        MDC.put("traceId", "trace-123");
        try {
            Message processed = processor.postProcessMessage(message);
            assertThat(processed.getMessageProperties().getHeaders().get("X-Trace-Id")).isEqualTo("trace-123");
        } finally {
            MDC.clear();
        }
    }

    @Test
    void listenerInterceptorShouldRestoreTraceIdIntoMdcForInvocationScope() throws Throwable {
        RabbitConfig config = new RabbitConfig();
        MethodInterceptor interceptor = config.rabbitListenerTraceMdcInterceptor();
        Message message = MessageBuilder.withBody("payload".getBytes())
                .setHeader("X-Trace-Id", "trace-456")
                .build();

        Object result = interceptor.invoke(new StubMethodInvocation(new Object[] {message}, () -> {
            assertThat(MDC.get("traceId")).isEqualTo("trace-456");
            return "ok";
        }));

        assertThat(result).isEqualTo("ok");
        assertThat(MDC.get("traceId")).isNull();
    }

    private static final class StubMethodInvocation implements MethodInvocation {
        private final Object[] arguments;
        private final ThrowingSupplier supplier;

        private StubMethodInvocation(Object[] arguments, ThrowingSupplier supplier) {
            this.arguments = arguments;
            this.supplier = supplier;
        }

        @Override
        public Method getMethod() {
            try {
                return StubMethodInvocation.class.getDeclaredMethod("proceed");
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public Object[] getArguments() {
            return arguments;
        }

        @Override
        public Object proceed() throws Throwable {
            return supplier.get();
        }

        @Override
        public Object getThis() {
            return this;
        }

        @Override
        public AccessibleObject getStaticPart() {
            return getMethod();
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get() throws Throwable;
    }
}
