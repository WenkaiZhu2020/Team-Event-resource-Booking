package com.teamresource.gateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final JsonAuthenticationEntryPoint entryPoint;

    public JsonAccessDeniedHandler(JsonAuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return entryPoint.writeError(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", denied.getMessage());
    }
}
