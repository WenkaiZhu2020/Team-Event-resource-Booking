package com.teamresource.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.analytics.AnalyticsServiceApplication;
import com.teamresource.analytics.api.GlobalExceptionHandler;
import com.teamresource.analytics.api.dto.ApiResponse;
import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.api.dto.EventRegistrationMetricResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.api.dto.ResourceUsageMetricResponse;
import com.teamresource.analytics.config.AnalyticsProperties;
import com.teamresource.analytics.config.AnalyticsServiceConfiguration;
import com.teamresource.analytics.config.OpenApiConfig;
import com.teamresource.analytics.config.RabbitConfig;
import com.teamresource.common.security.InternalApiProperties;
import com.teamresource.common.security.JwtProperties;
import com.teamresource.analytics.domain.BookingAnalyticsStatus;
import com.teamresource.analytics.infra.messaging.AnalyticsEventConsumer;
import com.teamresource.analytics.infra.messaging.DomainEventMessage;
import com.teamresource.analytics.infra.persistence.BookingFactEntity;
import com.teamresource.analytics.infra.persistence.BookingFactRepository;
import com.teamresource.analytics.infra.persistence.ConsumedEventEntity;
import com.teamresource.analytics.infra.persistence.ConsumedEventRepository;
import com.teamresource.analytics.infra.persistence.ResourcePopularityEntity;
import com.teamresource.analytics.infra.persistence.ResourcePopularityRepository;
import com.teamresource.analytics.service.strategy.AnalyticsAggregationStrategy;
import io.swagger.v3.oas.models.OpenAPI;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class AnalyticsModuleCoverageTest {

    @Test
    void applicationMainShouldDelegateToSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = org.mockito.Mockito.mockStatic(SpringApplication.class)) {
            AnalyticsServiceApplication.main(new String[]{"--test"});
            springApplication.verify(() -> SpringApplication.run(AnalyticsServiceApplication.class, new String[]{"--test"}));
        }
    }

    @Test
    void recordsEntitiesAndBuilderShouldExposeState() {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-30T12:00:00Z");

        ApiResponse<String> response = ApiResponse.of("ok");
        assertThat(response.data()).isEqualTo("ok");

        DashboardOverviewResponse overview = new DashboardOverviewBuilder()
                .totalBookings(1)
                .approvedBookings(2)
                .pendingApprovals(3)
                .waitlistedBookings(4)
                .cancelledOrRejectedBookings(5)
                .uniqueResourcesUsed(6)
                .nextSevenDaysApprovedBookings(7)
                .totalApprovedReservedMinutes(8)
                .build();
        assertThat(overview.totalApprovedReservedMinutes()).isEqualTo(8);
        assertThat(new EventRegistrationMetricResponse(UUID.randomUUID(), 1, 2, 3).cancelledBookings()).isEqualTo(3);
        assertThat(new ResourceUsageMetricResponse(UUID.randomUUID(), 1, 2, 3, 4, 5).bookedMinutes()).isEqualTo(5);
        assertThat(new ResourcePopularityResponse(UUID.randomUUID(), "Desk", "DESK", 1, 2, 3, 4, 5, 6, BigDecimal.ONE, now).resourceType())
                .isEqualTo("DESK");

        AnalyticsProperties analyticsProperties = new AnalyticsProperties("queue", "dlq", 1000L, 2);
        JwtProperties jwtProperties = new JwtProperties("issuer", Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes()), null);
        InternalApiProperties internalApiProperties = new InternalApiProperties("X-Key", "secret");
        assertThat(analyticsProperties.queueName()).isEqualTo("queue");
        assertThat(jwtProperties.issuer()).isEqualTo("issuer");
        assertThat(internalApiProperties.keyValue()).isEqualTo("secret");

        BookingFactEntity bookingFactEntity = new BookingFactEntity();
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID linkedEventId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        bookingFactEntity.setBookingId(bookingId);
        bookingFactEntity.setUserId(userId);
        bookingFactEntity.setLinkedEventId(linkedEventId);
        bookingFactEntity.setResourceId(resourceId);
        bookingFactEntity.setResourceName("Room A");
        bookingFactEntity.setResourceType("ROOM");
        bookingFactEntity.setBookingStatus(BookingAnalyticsStatus.APPROVED);
        bookingFactEntity.setApprovalMode("AUTO");
        bookingFactEntity.setWaitlistPosition(1);
        bookingFactEntity.setStartAt(now);
        bookingFactEntity.setEndAt(now.plusHours(1));
        bookingFactEntity.setCreatedAt(now.minusDays(1));
        bookingFactEntity.setUpdatedAt(now);
        bookingFactEntity.setLastEventType("booking.created");
        bookingFactEntity.setLastEventAt(now);
        assertThat(bookingFactEntity.getBookingId()).isEqualTo(bookingId);
        assertThat(bookingFactEntity.getUserId()).isEqualTo(userId);
        assertThat(bookingFactEntity.getLinkedEventId()).isEqualTo(linkedEventId);
        assertThat(bookingFactEntity.getResourceId()).isEqualTo(resourceId);
        assertThat(bookingFactEntity.getResourceName()).isEqualTo("Room A");
        assertThat(bookingFactEntity.getResourceType()).isEqualTo("ROOM");
        assertThat(bookingFactEntity.getBookingStatus()).isEqualTo(BookingAnalyticsStatus.APPROVED);
        assertThat(bookingFactEntity.getApprovalMode()).isEqualTo("AUTO");
        assertThat(bookingFactEntity.getWaitlistPosition()).isEqualTo(1);
        assertThat(bookingFactEntity.getStartAt()).isEqualTo(now);
        assertThat(bookingFactEntity.getEndAt()).isEqualTo(now.plusHours(1));
        assertThat(bookingFactEntity.getCreatedAt()).isEqualTo(now.minusDays(1));
        assertThat(bookingFactEntity.getUpdatedAt()).isEqualTo(now);
        assertThat(bookingFactEntity.getLastEventType()).isEqualTo("booking.created");
        assertThat(bookingFactEntity.getLastEventAt()).isEqualTo(now);

        ConsumedEventEntity consumedEventEntity = new ConsumedEventEntity();
        UUID messageId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        consumedEventEntity.setMessageId(messageId);
        consumedEventEntity.setAggregateType("booking");
        consumedEventEntity.setAggregateId(aggregateId);
        consumedEventEntity.setEventType("booking.created");
        consumedEventEntity.setConsumedAt(now);
        assertThat(consumedEventEntity.getMessageId()).isEqualTo(messageId);
        assertThat(consumedEventEntity.getAggregateType()).isEqualTo("booking");
        assertThat(consumedEventEntity.getAggregateId()).isEqualTo(aggregateId);
        assertThat(consumedEventEntity.getEventType()).isEqualTo("booking.created");
        assertThat(consumedEventEntity.getConsumedAt()).isEqualTo(now);

        ResourcePopularityEntity popularityEntity = new ResourcePopularityEntity();
        UUID popularityResourceId = UUID.randomUUID();
        popularityEntity.setResourceId(popularityResourceId);
        popularityEntity.setResourceName("Desk B");
        popularityEntity.setResourceType("DESK");
        popularityEntity.setTotalBookings(1);
        popularityEntity.setApprovedBookings(2);
        popularityEntity.setPendingBookings(3);
        popularityEntity.setWaitlistedBookings(4);
        popularityEntity.setCancelledBookings(5);
        popularityEntity.setTotalReservedMinutes(6);
        popularityEntity.setPopularityScore(BigDecimal.TEN);
        popularityEntity.setLastRefreshedAt(now);
        assertThat(popularityEntity.getResourceId()).isEqualTo(popularityResourceId);
        assertThat(popularityEntity.getResourceName()).isEqualTo("Desk B");
        assertThat(popularityEntity.getResourceType()).isEqualTo("DESK");
        assertThat(popularityEntity.getTotalBookings()).isEqualTo(1);
        assertThat(popularityEntity.getApprovedBookings()).isEqualTo(2);
        assertThat(popularityEntity.getPendingBookings()).isEqualTo(3);
        assertThat(popularityEntity.getWaitlistedBookings()).isEqualTo(4);
        assertThat(popularityEntity.getCancelledBookings()).isEqualTo(5);
        assertThat(popularityEntity.getTotalReservedMinutes()).isEqualTo(6);
        assertThat(popularityEntity.getPopularityScore()).isEqualTo(BigDecimal.TEN);
        assertThat(popularityEntity.getLastRefreshedAt()).isEqualTo(now);
        assertThat(BookingAnalyticsStatus.valueOf("UNKNOWN")).isEqualTo(BookingAnalyticsStatus.UNKNOWN);
    }

    @Test
    void configBeansShouldBuild() {
        AnalyticsServiceConfiguration serviceConfiguration = new AnalyticsServiceConfiguration();
        Executor executor = (Executor) invokeBeanMethod(
                serviceConfiguration,
                "analyticsTaskExecutor",
                new Class[]{AnalyticsProperties.class},
                new Object[]{new AnalyticsProperties("queue", "dlq", 1000L, 1)});
        assertThat(executor).isInstanceOf(TaskExecutorAdapter.class);

        OpenAPI openAPI = (OpenAPI) invokeBeanMethod(new OpenApiConfig(), "analyticsOpenApi", new Class[0], new Object[0]);
        assertThat(openAPI.getInfo().getTitle()).contains("Analytics");
        assertThat(openAPI.getSecurity()).hasSize(1);

        RabbitConfig rabbitConfig = new RabbitConfig();
        AnalyticsProperties properties = new AnalyticsProperties("queue", "dlq", 1000L, 1);
        TopicExchange topicExchange = (TopicExchange) invokeBeanMethod(rabbitConfig, "domainEventsExchange", new Class[0], new Object[0]);
        DirectExchange deadLetterExchange = (DirectExchange) invokeBeanMethod(rabbitConfig, "analyticsDeadLetterExchange", new Class[0], new Object[0]);
        Queue queue = (Queue) invokeBeanMethod(rabbitConfig, "analyticsQueue", new Class[]{AnalyticsProperties.class}, new Object[]{properties});
        Queue deadLetterQueue = (Queue) invokeBeanMethod(rabbitConfig, "analyticsDeadLetterQueue", new Class[]{AnalyticsProperties.class}, new Object[]{properties});
        Binding bookingBinding = (Binding) invokeBeanMethod(rabbitConfig, "analyticsBookingBinding", new Class[]{Queue.class, TopicExchange.class}, new Object[]{queue, topicExchange});
        Binding workflowBinding = (Binding) invokeBeanMethod(rabbitConfig, "analyticsWorkflowBinding", new Class[]{Queue.class, TopicExchange.class}, new Object[]{queue, topicExchange});
        Binding deadLetterBinding = (Binding) invokeBeanMethod(rabbitConfig, "analyticsDeadLetterBinding", new Class[]{Queue.class, DirectExchange.class, AnalyticsProperties.class}, new Object[]{deadLetterQueue, deadLetterExchange, properties});
        Jackson2JsonMessageConverter converter = (Jackson2JsonMessageConverter) invokeBeanMethod(rabbitConfig, "jackson2JsonMessageConverter", new Class[]{ObjectMapper.class}, new Object[]{new ObjectMapper()});
        org.aopalliance.intercept.MethodInterceptor interceptor = (org.aopalliance.intercept.MethodInterceptor) invokeBeanMethod(
                rabbitConfig,
                "rabbitListenerTraceMdcInterceptor",
                new Class[0],
                new Object[0]);
        SimpleRabbitListenerContainerFactory factory = (SimpleRabbitListenerContainerFactory) invokeBeanMethod(
                rabbitConfig,
                "rabbitListenerContainerFactory",
                new Class[]{ConnectionFactory.class, Jackson2JsonMessageConverter.class, org.aopalliance.intercept.MethodInterceptor.class, boolean.class},
                new Object[]{mock(ConnectionFactory.class), converter, interceptor, true});

        assertThat(topicExchange.getName()).isEqualTo(RabbitConfig.DOMAIN_EVENTS_EXCHANGE);
        assertThat(deadLetterExchange.getName()).isEqualTo("analytics.dead-letter");
        assertThat(queue.getName()).isEqualTo("queue");
        assertThat(deadLetterQueue.getName()).isEqualTo("dlq");
        assertThat(bookingBinding.getRoutingKey()).isEqualTo("booking.*");
        assertThat(workflowBinding.getRoutingKey()).isEqualTo("workflow.*");
        assertThat(deadLetterBinding.getRoutingKey()).isEqualTo("dlq");
        assertThat(factory).isNotNull();
    }

    @Test
    void dispatcherConsumerRefreshAndExceptionHandlerShouldWork() throws Exception {
        DomainEventMessage message = new DomainEventMessage(
                UUID.randomUUID(),
                "booking",
                UUID.randomUUID(),
                "booking.created",
                new ObjectMapper().valueToTree(Map.of("bookingId", UUID.randomUUID().toString())),
                OffsetDateTime.parse("2026-04-30T12:00:00Z"));

        AnalyticsAggregationStrategy strategy = mock(AnalyticsAggregationStrategy.class);
        when(strategy.supports(message)).thenReturn(true);
        AnalyticsEventDispatcher dispatcher = new AnalyticsEventDispatcher(List.of(strategy));
        dispatcher.dispatch(message);
        verify(strategy).apply(message);
        assertThatThrownBy(() -> new AnalyticsEventDispatcher(List.of()).dispatch(message))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported analytics event type");

        AnalyticsEventService eventService = new AnalyticsEventService(mock(ConsumedEventRepository.class), dispatcher);
        AnalyticsEventConsumer consumer = new AnalyticsEventConsumer(eventService);

        ConsumedEventRepository duplicateRepo = mock(ConsumedEventRepository.class);
        doThrow(new DataIntegrityViolationException("duplicate")).when(duplicateRepo).save(any());
        AnalyticsEventService duplicateSafeService = new AnalyticsEventService(duplicateRepo, dispatcher);
        duplicateSafeService.consume(message);

        ResourcePopularityRepository resourcePopularityRepository = mock(ResourcePopularityRepository.class);
        BookingFactRepository bookingFactRepository = mock(BookingFactRepository.class);
        BookingFactRepository.ResourcePopularityProjection projection = new BookingFactRepository.ResourcePopularityProjection() {
            @Override public UUID getResourceId() { return UUID.randomUUID(); }
            @Override public String getResourceName() { return "Room C"; }
            @Override public String getResourceType() { return "ROOM"; }
            @Override public long getTotalBookings() { return 10; }
            @Override public long getApprovedBookings() { return 5; }
            @Override public long getPendingBookings() { return 2; }
            @Override public long getWaitlistedBookings() { return 1; }
            @Override public long getCancelledBookings() { return 2; }
            @Override public long getTotalReservedMinutes() { return 180; }
        };
        when(bookingFactRepository.summarizeResourcePopularity()).thenReturn(List.of(projection));
        ResourcePopularityRefreshService refreshService = new ResourcePopularityRefreshService(bookingFactRepository, resourcePopularityRepository);
        refreshService.refresh();
        verify(resourcePopularityRepository).deleteAllInBatch();
        verify(resourcePopularityRepository).saveAll(any());

        consumer.onDomainEvent(message);

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertThat(handler.handleStatus(new ResponseStatusException(BAD_REQUEST, "bad")).getStatusCode().value()).isEqualTo(400);
        assertThat(handler.handleStatus(new ResponseStatusException(BAD_REQUEST)).getBody().message()).isEqualTo("Bad Request");
        assertThat(handler.handleUnhandled(new IllegalStateException("boom")).getStatusCode().value()).isEqualTo(500);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new LimitBean(), "payload");
        bindingResult.rejectValue("limit", "Min", "must be greater than or equal to 1");
        MethodArgumentNotValidException notValidException = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(
                        AnalyticsModuleCoverageTest.class.getDeclaredMethod("sampleMethod", String.class), 0),
                bindingResult);
        assertThat(handler.handleValidation(notValidException).getStatusCode().value()).isEqualTo(400);

        var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        class LimitHolder {
            @jakarta.validation.constraints.Min(2)
            int limit = 1;
        }
        var violations = validator.validate(new LimitHolder());
        assertThat(handler.handleConstraintViolation(new jakarta.validation.ConstraintViolationException(violations)).getStatusCode().value()).isEqualTo(400);
    }

    @SuppressWarnings("unused")
    private static void sampleMethod(String value) {
    }

    static class LimitBean {
        private int limit;

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }
    }

    private static Object invokeBeanMethod(Object target, String name, Class<?>[] parameterTypes, Object[] args) {
        try {
            var method = target.getClass().getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
