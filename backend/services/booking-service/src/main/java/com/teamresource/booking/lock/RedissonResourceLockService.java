package com.teamresource.booking.lock;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

public class RedissonResourceLockService implements ResourceLockService {

    private static final long WAIT_TIME_SECONDS = 3L;
    private static final long LEASE_TIME_SECONDS = 10L;
    private static final String LOCK_KEY_PREFIX = "booking-service:resource:";

    private final RedissonClient redissonClient;

    public RedissonResourceLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T executeWithResourceLock(UUID resourceId, Supplier<T> action) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + resourceId);
        boolean locked = false;
        boolean unlockOnTransactionCompletion = false;

        try {
            locked = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);
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
