package com.teamresource.notification.application.template;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SimpleTemplateRenderer implements TemplateRenderer {

    @Override
    public String render(String template, Map<String, Object> values) {
        String rendered = template;
        if (values == null || values.isEmpty()) {
            return rendered;
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            rendered = rendered.replace(key, value);
        }
        return rendered;
    }
}
