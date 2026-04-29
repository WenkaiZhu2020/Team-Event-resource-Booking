package com.teamresource.booking.messaging;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class BookingOutboxPublisherTestSkeleton {

    @Test
    @Disabled("Implement unit test for outbox event serialization and publish marking")
    void shouldPublishPendingOutboxEventsAndMarkAsPublished() {
    }

    @Test
    @Disabled("Implement retry test for transient RabbitMQ publish failure")
    void shouldRetryOutboxPublishOnTransientFailure() {
    }
}
