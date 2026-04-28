package com.teamresource.notification.application.template;

import java.util.Map;

public interface TemplateRenderer {

    String render(String template, Map<String, Object> values);
}
