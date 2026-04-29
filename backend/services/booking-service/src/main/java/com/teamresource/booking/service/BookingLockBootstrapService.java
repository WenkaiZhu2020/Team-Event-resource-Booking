package com.teamresource.booking.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class BookingLockBootstrapService {

    private final JdbcTemplate jdbcTemplate;

    public BookingLockBootstrapService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureLockExists(UUID resourceId) {
        try {
            jdbcTemplate.update(
                    "insert into bookings.booking_locks (resource_id, updated_at) values (?, ?)",
                    resourceId,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
        } catch (DataIntegrityViolationException ignored) {
        }
    }
}
