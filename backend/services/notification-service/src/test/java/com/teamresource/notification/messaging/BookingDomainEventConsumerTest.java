package com.teamresource.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamresource.notification.infra.messaging.BookingNotificationConsumer;
import com.teamresource.notification.infra.persistence.NotificationRecordRepository;
import com.teamresource.notification.infra.persistence.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BookingDomainEventConsumerTest {

    @Autowired
    private BookingNotificationConsumer consumer;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldConsumeMessageIdempotently() throws Exception {
        long notificationsBefore = notificationRecordRepository.count();
        long consumedBefore = processedEventRepository.count();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", "BOOKING_CONFIRMED");
        payload.put("bookingId", "4fa4c429-672d-4c12-aefe-88ea3bb5483f");
        payload.put("userId", "bc9fbeef-5792-4f46-b922-7613e653f848");
        payload.put("resourceId", "80eaec43-b7ec-4e79-8615-c87b3eec7127");
        payload.put("startAt", "2026-05-01T10:00:00Z");
        payload.put("endAt", "2026-05-01T11:00:00Z");

        var message = new com.teamresource.notification.infra.messaging.DomainEventMessage(
                java.util.UUID.randomUUID(),
                "BOOKING",
                java.util.UUID.fromString("4fa4c429-672d-4c12-aefe-88ea3bb5483f"),
                "booking.approved",
                payload,
                java.time.OffsetDateTime.parse("2026-05-01T09:00:00Z")
        );

        consumer.onBookingEvent(message);
        consumer.onBookingEvent(message);

        assertThat(processedEventRepository.count()).isEqualTo(consumedBefore + 1);
        assertThat(notificationRecordRepository.count()).isEqualTo(notificationsBefore + 4);
    }
}
