package com.teamresource.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamresource.gateway.error.ApiErrorAttributes;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

class GatewayFilterAndErrorTest {

    @Test
    void traceIdFilterShouldReuseOrCreateTraceHeader() {
        TraceIdFilter filter = new TraceIdFilter();
        MockServerWebExchange exchangeWithHeader = MockServerWebExchange.from(
                MockServerHttpRequest.get("/gateway").header(TraceIdFilter.TRACE_ID_HEADER, "existing-trace")
        );

        final String[] downstreamHeader = new String[1];
        filter.filter(exchangeWithHeader, ex -> {
            downstreamHeader[0] = ex.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
            return Mono.empty();
        }).block();

        assertThat(exchangeWithHeader.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("existing-trace");
        assertThat(downstreamHeader[0]).isEqualTo("existing-trace");
        assertThat(filter.getOrder()).isEqualTo(Integer.MIN_VALUE);

        MockServerWebExchange exchangeWithoutHeader = MockServerWebExchange.from(MockServerHttpRequest.get("/gateway"));
        final String[] generatedDownstreamHeader = new String[1];
        filter.filter(exchangeWithoutHeader, ex -> {
            generatedDownstreamHeader[0] = ex.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
            return Mono.empty();
        }).block();

        assertThat(exchangeWithoutHeader.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER)).isNotBlank();
        assertThat(generatedDownstreamHeader[0]).isEqualTo(exchangeWithoutHeader.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER));

        MockServerWebExchange exchangeWithBlankHeader = MockServerWebExchange.from(
                MockServerHttpRequest.get("/gateway").header(TraceIdFilter.TRACE_ID_HEADER, "   ")
        );
        final String[] blankDownstreamHeader = new String[1];
        filter.filter(exchangeWithBlankHeader, ex -> {
            blankDownstreamHeader[0] = ex.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
            return Mono.empty();
        }).block();

        assertThat(exchangeWithBlankHeader.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER)).isNotBlank();
        assertThat(blankDownstreamHeader[0]).isEqualTo(exchangeWithBlankHeader.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    void apiErrorAttributesShouldReturnGatewayPayload() {
        ApiErrorAttributes attributes = new ApiErrorAttributes();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/failure"));
        attributes.storeErrorInformation(new IllegalStateException("boom"), exchange);
        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        Map<String, Object> error = attributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());

        assertThat(error.get("status")).isEqualTo(500);
        assertThat(error.get("path")).isEqualTo("/failure");
        assertThat(error).containsKey("requestId");
    }
}
