package com.teamresource.gateway.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> defaults = super.getErrorAttributes(request, options);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now());
        payload.put("status", defaults.get("status"));
        payload.put("error", defaults.get("error"));
        payload.put("message", defaults.getOrDefault("message", "Unexpected gateway error"));
        payload.put("path", request.path());
        payload.put("requestId", request.exchange().getRequest().getId());
        return payload;
    }
}
