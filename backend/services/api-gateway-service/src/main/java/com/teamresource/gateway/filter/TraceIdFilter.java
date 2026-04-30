package com.teamresource.gateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        String propagatedTraceId = traceId;
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(TRACE_ID_HEADER, propagatedTraceId)))
                .build();
        mutatedExchange.getAttributes().put(TRACE_ID_HEADER, propagatedTraceId);
        mutatedExchange.getResponse().getHeaders().set(TRACE_ID_HEADER, propagatedTraceId);
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
