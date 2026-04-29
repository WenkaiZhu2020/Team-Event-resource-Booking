package com.teamresource.notification.config;

import com.teamresource.notification.infrastructure.config.ChannelProperties;
import com.teamresource.notification.infrastructure.config.MessagingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ChannelProperties.class, MessagingProperties.class})
public class ChannelPropertiesConfiguration {
}
