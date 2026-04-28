package com.teamresource.booking.infrastructure.persistence.repository;

import com.teamresource.booking.domain.repository.ResourceBookingLockRepository;
import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.infrastructure.persistence.entity.ResourceBookingLockEntity;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResourceBookingLockRepositoryImpl implements ResourceBookingLockRepository {

    private final SpringDataResourceBookingLockJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final boolean postgresDialect;

    public ResourceBookingLockRepositoryImpl(
            SpringDataResourceBookingLockJpaRepository repository,
            DataSource dataSource
    ) {
        this.repository = repository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.postgresDialect = isPostgres(dataSource);
    }

    @Override
    public ResourceBookingLockEntity acquireLock(UUID resourceId) {
        insertIfAbsent(resourceId);

        ResourceBookingLockEntity locked = repository.lockByResourceId(resourceId);
        if (locked == null) {
            throw new ApiException(
                    "RESOURCE_LOCK_ACQUIRE_FAILED",
                    HttpStatus.CONFLICT,
                    "Unable to acquire booking lock for resource " + resourceId
            );
        }
        return locked;
    }

    private void insertIfAbsent(UUID resourceId) {
        if (postgresDialect) {
            jdbcTemplate.update(
                    "insert into bookings.resource_booking_locks(resource_id, version) values (?, 0) on conflict (resource_id) do nothing",
                    resourceId
            );
            return;
        }
        jdbcTemplate.update(
                "merge into bookings.resource_booking_locks(resource_id, version) key(resource_id) values (?, ?)",
                resourceId,
                0L
        );
    }

    private boolean isPostgres(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase().contains("postgres");
        } catch (SQLException ex) {
            throw new ApiException(
                    "RESOURCE_LOCK_DB_DETECT_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to detect database dialect"
            );
        }
    }
}
