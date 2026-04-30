package com.teamresource.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.test.StepVerifier;

class GatewaySecurityComponentTest {

    @Test
    void jwtRoleAuthenticationConverterShouldMapRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .subject("user-1")
                .header("alg", "HS256")
                .claim("roles", List.of("USER", "ROLE_ADMIN"))
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        var authentication = new JwtRoleAuthenticationConverter().convert(jwt).block();

        assertThat(authentication.getName()).isEqualTo("user-1");
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void jwtRoleAuthenticationConverterShouldHandleMissingRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .subject("user-1")
                .header("alg", "HS256")
                .claim("roles", "USER")
                .build();

        var authentication = new JwtRoleAuthenticationConverter().convert(jwt).block();

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void jsonHandlersShouldWriteJsonErrors() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        JsonAccessDeniedHandler deniedHandler = new JsonAccessDeniedHandler(entryPoint);

        MockServerWebExchange unauthorizedExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/secure"));
        StepVerifier.create(entryPoint.commence(unauthorizedExchange, new BadCredentialsException("bad token"))).verifyComplete();
        String unauthorizedBody = unauthorizedExchange.getResponse().getBodyAsString().block();

        assertThat(unauthorizedExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthorizedExchange.getResponse().getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_JSON);
        assertThat(unauthorizedBody).contains("UNAUTHORIZED");

        MockServerWebExchange forbiddenExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/secure"));
        StepVerifier.create(deniedHandler.handle(forbiddenExchange, new org.springframework.security.access.AccessDeniedException("denied"))).verifyComplete();
        String forbiddenBody = forbiddenExchange.getResponse().getBodyAsString().block();

        assertThat(forbiddenExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbiddenExchange.getResponse().getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_JSON);
        assertThat(forbiddenBody).contains("FORBIDDEN");
    }

    @Test
    void jsonAuthenticationEntryPointShouldCompleteOnSerializationFailure() {
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) {
                throw new RuntimeException("boom");
            }
        });

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/secure"));
        StepVerifier.create(entryPoint.writeError(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "bad token")).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
