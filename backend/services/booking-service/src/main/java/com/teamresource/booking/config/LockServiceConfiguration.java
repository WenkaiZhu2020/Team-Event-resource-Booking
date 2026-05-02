package com.teamresource.booking.config;

import com.teamresource.booking.lock.InMemoryResourceLockService;
import com.teamresource.booking.lock.RedissonResourceLockService;
import com.teamresource.booking.lock.ResourceLockService;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LockServiceConfiguration {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    ResourceLockService redissonResourceLockService(RedissonClient redissonClient) {
        return new RedissonResourceLockService(redissonClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false")
    @ConditionalOnMissingBean(ResourceLockService.class)
    ResourceLockService inMemoryResourceLockService() {
        return new InMemoryResourceLockService();
    }
}
