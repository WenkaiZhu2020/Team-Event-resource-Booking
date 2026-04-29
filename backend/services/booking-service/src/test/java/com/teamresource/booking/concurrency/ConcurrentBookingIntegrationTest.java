package com.teamresource.booking.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import com.teamresource.booking.config.ClientProperties;
import com.teamresource.booking.infra.client.EventClient;
import com.teamresource.booking.infra.client.ResourceClient;
import com.teamresource.booking.infra.client.WorkflowClient;
import com.teamresource.booking.service.BookingFacade;
import com.teamresource.booking.service.command.CreateBookingCommand;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {com.teamresource.booking.BookingServiceApplication.class, ConcurrentBookingIntegrationTest.TestConfig.class})
class ConcurrentBookingIntegrationTest {

    @Autowired
    private BookingFacade bookingFacade;

    @Autowired
    private ResourceClient resourceClient;

    @Autowired
    private EventClient eventClient;

    @Autowired
    private WorkflowClient workflowClient;

    @Test
    void shouldCreateOneConfirmedAndOneWaitlistedWhenConcurrentRequestsConflict() throws Exception {
        UUID resourceId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endAt = startAt.plusHours(2);

        ((StubResourceClient) resourceClient).resource = new ResourceClient.ResourceSnapshot(
                resourceId,
                managerId,
                "Room A",
                "Quiet room",
                "ROOM",
                "B1",
                8,
                "ACTIVE",
                "AUTO_APPROVE",
                false,
                true,
                240,
                30,
                List.of(new ResourceClient.AvailabilityRuleSnapshot(UUID.randomUUID(), startAt.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(18, 0), true)),
                List.of(),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<BookingResponse> task1 = () -> {
            latch.await();
            return bookingFacade.create(new CreateBookingCommand(
                    UUID.randomUUID(),
                    new CreateBookingRequest(resourceId, null, startAt, endAt, "Concurrent booking 1"),
                    "idem-key-1"
            ));
        };

        Callable<BookingResponse> task2 = () -> {
            latch.await();
            return bookingFacade.create(new CreateBookingCommand(
                    UUID.randomUUID(),
                    new CreateBookingRequest(resourceId, null, startAt, endAt, "Concurrent booking 2"),
                    "idem-key-2"
            ));
        };

        Future<BookingResponse> future1 = executor.submit(task1);
        Future<BookingResponse> future2 = executor.submit(task2);
        latch.countDown();

        BookingResponse r1 = future1.get();
        BookingResponse r2 = future2.get();
        executor.shutdownNow();

        List<String> statuses = List.of(r1.status(), r2.status());
        assertThat(statuses).containsExactlyInAnyOrder("APPROVED", "WAITLISTED");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        ResourceClient resourceClient() {
            return new StubResourceClient();
        }

        @Bean
        @Primary
        EventClient eventClient() {
            return new StubEventClient();
        }

        @Bean
        @Primary
        WorkflowClient workflowClient() {
            return new StubWorkflowClient();
        }
    }

    static class StubResourceClient extends ResourceClient {
        private ResourceSnapshot resource;

        StubResourceClient() {
            super(null);
        }

        @Override
        public ResourceSnapshot getResource(UUID resourceId) {
            return resource;
        }
    }

    static class StubEventClient extends EventClient {

        StubEventClient() {
            super(null);
        }
    }

    static class StubWorkflowClient extends WorkflowClient {

        StubWorkflowClient() {
            super(null, new ClientProperties("X-Internal-Api-Key", "test-auth-key", "test-user-key", "test-workflow-key", "test-event-key"));
        }

        @Override
        public void createBookingApproval(BookingResponse booking) {
        }
    }
}
