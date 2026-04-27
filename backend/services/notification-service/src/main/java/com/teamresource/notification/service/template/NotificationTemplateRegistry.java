package com.teamresource.notification.service.template;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class NotificationTemplateRegistry {

    private final List<NotificationTemplateRenderer> renderers;

    public NotificationTemplateRegistry(List<NotificationTemplateRenderer> renderers) {
        this.renderers = renderers;
    }

    public NotificationTemplateRenderer resolve(String eventType) {
        return renderers.stream()
                .filter(renderer -> renderer.supports(eventType))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported notification event type: " + eventType));
    }
}
