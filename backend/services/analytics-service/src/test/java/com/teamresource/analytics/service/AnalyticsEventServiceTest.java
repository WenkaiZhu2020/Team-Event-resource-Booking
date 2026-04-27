package com.teamresource.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.analytics.infra.messaging.DomainEventMessage;
import com.teamresource.analytics.infra.persistence.ConsumedEventRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class AnalyticsEventServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateEventShouldNotDispatchTwice() {
        ConsumedEventRepository consumedEventRepository = mock(ConsumedEventRepository.class);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(consumedEventRepository)
                .save(any());

        RecordingAnalyticsEventDispatcher analyticsEventDispatcher = new RecordingAnalyticsEventDispatcher();
        AnalyticsEventService service = new AnalyticsEventService(consumedEventRepository, analyticsEventDispatcher);

        DomainEventMessage message = new DomainEventMessage(
                UUID.randomUUID(),
                "booking",
                UUID.randomUUID(),
                "booking.created",
                objectMapper.valueToTree(Map.of("userId", UUID.randomUUID())),
                OffsetDateTime.now()
        );

        service.consume(message);

        assertThat(analyticsEventDispatcher.dispatched).isFalse();
    }

    static class RecordingAnalyticsEventDispatcher extends AnalyticsEventDispatcher {

        private boolean dispatched;

        RecordingAnalyticsEventDispatcher() {
            super(java.util.List.of());
        }

        @Override
        public void dispatch(DomainEventMessage eventMessage) {
            dispatched = true;
        }
    }
}
