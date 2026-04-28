package com.teamresource.notification.application.pipeline;

import com.teamresource.notification.domain.model.NotificationChannel;

public abstract class AbstractChannelSender implements ChannelSenderStrategy {

    @Override
    public final ChannelSendResult send(NotificationDispatchContext context) {
        long start = System.currentTimeMillis();
        try {
            preSend(context);
            String providerMessageId = doSend(context);
            postSend(context);
            return ChannelSendResult.success(providerMessageId, elapsed(start));
        } catch (Exception ex) {
            return ChannelSendResult.failure(trim(ex.getMessage()), elapsed(start));
        }
    }

    protected void preSend(NotificationDispatchContext context) {
        if (context == null || context.notification() == null) {
            throw new IllegalArgumentException("notification context is required");
        }
        if (context.notification().getChannel() != channel()) {
            throw new IllegalArgumentException("channel mismatch for sender " + channel());
        }
    }

    protected void postSend(NotificationDispatchContext context) {
        // extension point
    }

    protected abstract String doSend(NotificationDispatchContext context);

    private long elapsed(long start) {
        return Math.max(1L, System.currentTimeMillis() - start);
    }

    private String trim(String message) {
        if (message == null || message.isBlank()) {
            return "unknown delivery error";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
