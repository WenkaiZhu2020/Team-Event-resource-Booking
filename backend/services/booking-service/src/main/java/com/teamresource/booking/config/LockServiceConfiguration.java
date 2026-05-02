package com.teamresource.booking.config;

import com.teamresource.booking.lock.RedissonResourceLockService;
import com.teamresource.booking.lock.ResourceLockService;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LockServiceConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(RedissonClient.class)
    ResourceLockService redissonResourceLockService(RedissonClient redissonClient, RedisProperties properties) {
        return new RedissonResourceLockService(redissonClient, properties.lockWaitSeconds());
    }
}
