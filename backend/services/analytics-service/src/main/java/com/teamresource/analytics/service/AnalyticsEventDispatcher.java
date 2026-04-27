package com.teamresource.analytics.service;

import com.teamresource.analytics.infra.messaging.DomainEventMessage;
import com.teamresource.analytics.service.strategy.AnalyticsAggregationStrategy;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AnalyticsEventDispatcher {

    private final List<AnalyticsAggregationStrategy> strategies;

    public AnalyticsEventDispatcher(List<AnalyticsAggregationStrategy> strategies) {
        this.strategies = strategies;
    }

    public void dispatch(DomainEventMessage eventMessage) {
        strategies.stream()
                .filter(strategy -> strategy.supports(eventMessage))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported analytics event type: " + eventMessage.eventType()))
                .apply(eventMessage);
    }
}
