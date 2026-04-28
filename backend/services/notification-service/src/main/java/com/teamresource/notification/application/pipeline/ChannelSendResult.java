package com.teamresource.notification.application.pipeline;

public record ChannelSendResult(
        boolean success,
        String providerMessageId,
        String errorMessage,
        long durationMs
) {

    public static ChannelSendResult success(String providerMessageId, long durationMs) {
        return new ChannelSendResult(true, providerMessageId, null, durationMs);
    }

    public static ChannelSendResult failure(String errorMessage, long durationMs) {
        return new ChannelSendResult(false, null, errorMessage, durationMs);
    }
}
