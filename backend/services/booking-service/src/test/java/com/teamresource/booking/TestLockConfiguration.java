package com.teamresource.booking;

import com.teamresource.booking.lock.ResourceLockService;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@TestConfiguration
public class TestLockConfiguration {

    @Bean
    ResourceLockService resourceLockService() {
        return new TestInMemoryResourceLockService();
    }

    private static final class TestInMemoryResourceLockService implements ResourceLockService {

        private static final long WAIT_TIME_SECONDS = 3L;

        private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

        @Override
        public <T> T executeWithResourceLock(UUID resourceId, Supplier<T> action) {
            ReentrantLock lock = locks.computeIfAbsent(resourceId, ignored -> new ReentrantLock());
            boolean locked = false;
            boolean unlockOnTransactionCompletion = false;

            try {
                locked = lock.tryLock(WAIT_TIME_SECONDS, TimeUnit.SECONDS);
                if (!locked) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Timed out while acquiring the booking lock for resource " + resourceId
                    );
                }

                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    unlockOnTransactionCompletion = true;
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (lock.isHeldByCurrentThread()) {
                                lock.unlock();
                            }
                        }
                    });
                }

                return action.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Interrupted while acquiring the booking lock for resource " + resourceId
                );
            } finally {
                if (locked && !unlockOnTransactionCompletion && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}
