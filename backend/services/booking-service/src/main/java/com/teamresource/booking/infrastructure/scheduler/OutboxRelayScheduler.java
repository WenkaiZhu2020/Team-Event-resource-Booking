package com.teamresource.booking.infrastructure.scheduler;

import com.teamresource.booking.domain.model.OutboxStatus;
import com.teamresource.booking.domain.repository.OutboxRepository;
import com.teamresource.booking.infrastructure.config.MessagingProperties;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRelayScheduler {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    public OutboxRelayScheduler(
            OutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            MessagingProperties properties
    ) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.messaging.outbox.retry-delay-seconds:60}000")
    public void relay() {
        int batchSize = Math.max(1, properties.outbox().batchSize());

        publishBatch(outboxRepository.findByStatus(OutboxStatus.PENDING, batchSize));
        publishBatch(outboxRepository.findRetryableFailed(5, batchSize));
    }

    private void publishBatch(List<com.teamresource.booking.infrastructure.persistence.entity.BookingOutboxEntity> outboxes) {
        for (var outbox : outboxes) {
            try {
                rabbitTemplate.convertAndSend(properties.exchange(), "booking.event", outbox.getPayloadJson());
                outboxRepository.markPublished(outbox, OffsetDateTime.now(ZoneOffset.UTC));
            } catch (Exception ex) {
                outboxRepository.markFailed(outbox, trim(ex.getMessage()));
            }
        }
    }

    private String trim(String message) {
        if (message == null) {
            return "unknown";
        }
        if (message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}
