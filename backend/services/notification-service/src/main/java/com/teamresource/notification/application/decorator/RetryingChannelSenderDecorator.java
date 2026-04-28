package com.teamresource.notification.application.decorator;

import com.teamresource.notification.application.pipeline.ChannelSendResult;
import com.teamresource.notification.application.pipeline.ChannelSenderStrategy;
import com.teamresource.notification.application.pipeline.NotificationDispatchContext;
import com.teamresource.notification.domain.model.NotificationChannel;

public class RetryingChannelSenderDecorator implements ChannelSenderStrategy {

    private final ChannelSenderStrategy delegate;
    private final int attempts;

    public RetryingChannelSenderDecorator(ChannelSenderStrategy delegate, int attempts) {
        this.delegate = delegate;
        this.attempts = Math.max(1, attempts);
    }

    @Override
    public NotificationChannel channel() {
        return delegate.channel();
    }

    @Override
    public ChannelSendResult send(NotificationDispatchContext context) {
        ChannelSendResult last = null;
        for (int i = 0; i < attempts; i++) {
            last = delegate.send(context);
            if (last.success()) {
                return last;
            }
        }
        return last == null ? ChannelSendResult.failure("send failed", 1) : last;
    }
}
