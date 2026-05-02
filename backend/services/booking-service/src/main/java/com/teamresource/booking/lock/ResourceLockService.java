package com.teamresource.booking.lock;

import java.util.UUID;
import java.util.function.Supplier;

public interface ResourceLockService {

    <T> T executeWithResourceLock(UUID resourceId, Supplier<T> action);

    default void executeWithResourceLock(UUID resourceId, Runnable action) {
        executeWithResourceLock(resourceId, () -> {
            action.run();
            return null;
        });
    }
}
