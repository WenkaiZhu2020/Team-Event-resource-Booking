package com.teamresource.booking.domain.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
