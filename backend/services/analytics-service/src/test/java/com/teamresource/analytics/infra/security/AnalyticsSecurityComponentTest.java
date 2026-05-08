package com.teamresource.analytics.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.analytics.config.SecurityConfig;
import com.teamresource.common.security.InternalApiKeyFilter;
import com.teamresource.common.security.InternalApiProperties;
import com.teamresource.common.security.JsonAuthEntryPoint;
import com.teamresource.common.security.JwtAuthenticationFilter;
import com.teamresource.common.security.JwtProperties;
import com.teamresource.common.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AnalyticsSecurityComponentTest {

    private static final String SECRET_TEXT = "12345678901234567890123456789012";
    private static final String SECRET_BASE64 = Base64.getEncoder().encodeToString(SECRET_TEXT.getBytes(StandardCharsets.UTF_8));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void internalApiKeyFilterShouldAuthenticateMatchingRequests() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter(new InternalApiProperties("X-Internal-Api-Key", "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/internal/report");
        request.addHeader("X-Internal-Api-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_INTERNAL_SERVICE");
    }

    @Test
    void internalApiKeyFilterShouldIgnoreNonMatchingRequests() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter(new InternalApiProperties("X-Internal-Api-Key", "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/dashboard/overview");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void internalApiKeyFilterShouldIgnoreWrongKeyOnInternalPath() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter(new InternalApiProperties("X-Internal-Api-Key", "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/internal/report");
        request.addHeader("X-Internal-Api-Key", "wrong");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void internalApiKeyFilterShouldIgnoreBlankKeyOnInternalPath() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter(new InternalApiProperties("X-Internal-Api-Key", "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/internal/report");
        request.addHeader("X-Internal-Api-Key", "");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void jsonAuthEntryPointShouldWriteUnauthorizedPayload() throws Exception {
        JsonAuthEntryPoint entryPoint = new JsonAuthEntryPoint(new ObjectMapper().findAndRegisterModules());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/dashboard/overview");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new org.springframework.security.authentication.BadCredentialsException("bad token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized").contains("/api/v1/dashboard/overview");
    }

    @Test
    void jwtServiceAndFilterShouldParseAndAuthenticate() throws Exception {
        JwtService jwtService = new JwtService(new JwtProperties("issuer", SECRET_BASE64, null));
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
        String token = Jwts.builder()
                .issuer("issuer")
                .subject("user-1")
                .claim("roles", List.of("ADMIN", "ROLE_USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();

        assertThat(jwtService.parse(token).getSubject()).isEqualTo("user-1");

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void jwtFilterShouldIgnoreExistingOrInvalidAuthentication() throws Exception {
        JwtService jwtService = new JwtService(new JwtProperties("issuer", SECRET_BASE64, null));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("existing", null, List.of()));
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existing");

        SecurityContextHolder.clearContext();
        MockHttpServletRequest invalidRequest = new MockHttpServletRequest();
        invalidRequest.addHeader("Authorization", "Bearer invalid");
        filter.doFilter(invalidRequest, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest noBearerRequest = new MockHttpServletRequest();
        noBearerRequest.addHeader("Authorization", "Basic abc");
        filter.doFilter(noBearerRequest, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest missingHeaderRequest = new MockHttpServletRequest();
        filter.doFilter(missingHeaderRequest, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void jwtFilterShouldHandleMissingRolesClaimAsEmptyAuthorities() throws Exception {
        JwtService jwtService = new JwtService(new JwtProperties("issuer", SECRET_BASE64, null));
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
        String token = Jwts.builder()
                .issuer("issuer")
                .subject("user-2")
                .claim("roles", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
    }

    @Test
    void securityConfigShouldBuildFilterChain() throws Exception {
        var objectPostProcessor = new org.springframework.security.config.annotation.ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        var authenticationBuilder = new org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder(objectPostProcessor);
        var applicationContext = new org.springframework.web.context.support.StaticWebApplicationContext();
        applicationContext.refresh();
        applicationContext.getBeanFactory().registerSingleton(
                "mvcHandlerMappingIntrospector",
                new org.springframework.web.servlet.handler.HandlerMappingIntrospector());
        var http = new org.springframework.security.config.annotation.web.builders.HttpSecurity(
                objectPostProcessor,
                authenticationBuilder,
                new java.util.HashMap<>(java.util.Map.of(org.springframework.context.ApplicationContext.class, applicationContext)));

        SecurityConfig securityConfig = new SecurityConfig();
        var method = SecurityConfig.class.getDeclaredMethod(
                "securityFilterChain",
                org.springframework.security.config.annotation.web.builders.HttpSecurity.class,
                JwtAuthenticationFilter.class,
                InternalApiKeyFilter.class,
                JsonAuthEntryPoint.class);
        method.setAccessible(true);
        SecurityFilterChain chain = (SecurityFilterChain) method.invoke(
                securityConfig,
                http,
                new JwtAuthenticationFilter(new JwtService(new JwtProperties("issuer", SECRET_BASE64, null))),
                new InternalApiKeyFilter(new InternalApiProperties("X-Internal-Api-Key", "secret")),
                new JsonAuthEntryPoint(new ObjectMapper()));

        assertThat(chain).isNotNull();
    }
}
