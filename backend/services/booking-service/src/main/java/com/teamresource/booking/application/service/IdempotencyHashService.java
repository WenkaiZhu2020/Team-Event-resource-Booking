package com.teamresource.booking.application.service;

import com.teamresource.booking.application.command.CreateBookingCommand;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyHashService {

    public String hash(CreateBookingCommand command) {
        String payload = command.userId() + "|" + command.eventId() + "|" + command.resourceId() + "|"
                + command.startAt() + "|" + command.endAt();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
