package com.teamresource.analytics.service.strategy;

import com.teamresource.analytics.infra.messaging.DomainEventMessage;

public interface AnalyticsAggregationStrategy {

    boolean supports(DomainEventMessage eventMessage);

    void apply(DomainEventMessage eventMessage);
}
