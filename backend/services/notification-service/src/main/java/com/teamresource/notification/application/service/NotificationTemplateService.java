package com.teamresource.notification.application.service;

import com.teamresource.notification.application.template.TemplateRenderer;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.repository.NotificationTemplateRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateRenderer templateRenderer;

    public NotificationTemplateService(
            NotificationTemplateRepository templateRepository,
            TemplateRenderer templateRenderer
    ) {
        this.templateRepository = templateRepository;
        this.templateRenderer = templateRenderer;
    }

    public RenderedTemplate render(String templateCode, NotificationChannel channel, Map<String, Object> templateData) {
        return templateRepository.findActive(templateCode, channel)
                .map(template -> new RenderedTemplate(
                        templateRenderer.render(template.getTitleTemplate(), templateData),
                        templateRenderer.render(template.getBodyTemplate(), templateData)))
                .orElseGet(() -> fallback(templateCode, templateData));
    }

    private RenderedTemplate fallback(String templateCode, Map<String, Object> templateData) {
        String title = templateCode == null ? "Notification" : templateCode.replace('_', ' ');
        String body = templateData == null || templateData.isEmpty()
                ? "You have a new notification"
                : templateData.entrySet().stream()
                        .map(e -> e.getKey() + ": " + (e.getValue() == null ? "" : e.getValue()))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("You have a new notification");
        return new RenderedTemplate(title, body);
    }
}
