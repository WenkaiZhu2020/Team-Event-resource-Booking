package com.teamresource.booking.service;

import com.teamresource.booking.config.BookingServiceConfiguration;
import com.teamresource.booking.infra.persistence.OutboxMessageEntity;
import com.teamresource.booking.infra.persistence.OutboxMessageRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingOutboxPublisher {

    private final OutboxMessageRepository outboxMessageRepository;
    private final BookingOutboxService bookingOutboxService;
    private final RabbitTemplate rabbitTemplate;

    public BookingOutboxPublisher(
            OutboxMessageRepository outboxMessageRepository,
            BookingOutboxService bookingOutboxService,
            RabbitTemplate rabbitTemplate
    ) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.bookingOutboxService = bookingOutboxService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:5000}")
    @Transactional
    public void publishPendingMessages() {
        List<OutboxMessageEntity> messages = outboxMessageRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxMessageEntity message : messages) {
            rabbitTemplate.convertAndSend(
                    BookingServiceConfiguration.BOOKING_EVENTS_EXCHANGE,
                    message.getEventType(),
                    bookingOutboxService.toDomainEvent(message)
            );
            message.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
            outboxMessageRepository.save(message);
        }
    }
}
