package com.teamresource.user.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamresource.common.security.InternalApiKeyFilter;
import com.teamresource.common.security.InternalApiProperties;
import com.teamresource.common.security.JwtAuthenticationFilter;
import com.teamresource.common.security.JwtProperties;
import com.teamresource.common.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class UserSecurityComponentTest {

    private static final String SECRET = "VEVBTV9SRVNPVVJDRV9NQU5BR0VNRU5UX0RFVl9TRUNSRVRfSFM1Nl8zMl9CWVRFUw==";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtServiceShouldParseAndValidateTokens() {
        JwtService service = new JwtService(new JwtProperties("issuer", SECRET, null));
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String validToken = Jwts.builder()
                .subject("user-1")
                .claim("roles", List.of("USER"))
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key)
                .compact();
        String expiredToken = Jwts.builder()
                .subject("user-2")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        Claims claims = service.parse(validToken);
        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(service.isValid(validToken)).isTrue();
        assertThat(service.isValid(expiredToken)).isFalse();
        assertThat(service.isValid("bad-token")).isFalse();
    }

    @Test
    void jwtAuthenticationFilterShouldPopulateAuthorities() throws Exception {
        JwtService service = new JwtService(new JwtProperties("issuer", SECRET, null));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service);
        String token = signedToken("user-123", List.of("USER", "ROLE_ADMIN"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user-123");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void jwtAuthenticationFilterShouldSkipInvalidTokens() throws Exception {
        JwtService service = new JwtService(new JwtProperties("issuer", SECRET, null));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service);

        MockHttpServletRequest basicRequest = new MockHttpServletRequest();
        basicRequest.setRequestURI("/api/v1/users/me");
        basicRequest.addHeader("Authorization", "Basic abc");
        filter.doFilter(basicRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest invalidBearerRequest = new MockHttpServletRequest();
        invalidBearerRequest.setRequestURI("/api/v1/users/me");
        invalidBearerRequest.addHeader("Authorization", "Bearer bad-token");
        filter.doFilter(invalidBearerRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void jwtAuthenticationFilterShouldIgnoreMissingHeaderAndNonListRoles() throws Exception {
        JwtService service = new JwtService(new JwtProperties("issuer", SECRET, null));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service);

        MockHttpServletRequest missingHeaderRequest = new MockHttpServletRequest();
        missingHeaderRequest.setRequestURI("/api/v1/users/me");
        filter.doFilter(missingHeaderRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .subject("user-roles-string")
                .claim("roles", "USER")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key)
                .compact();

        MockHttpServletRequest stringRolesRequest = new MockHttpServletRequest();
        stringRolesRequest.setRequestURI("/api/v1/users/me");
        stringRolesRequest.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(stringRolesRequest, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
    }

    @Test
    void internalApiKeyFilterShouldAuthenticateInternalRequests() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter(new InternalApiProperties("X-Internal-Api-Key", "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/internal/users/provision");
        request.addHeader("X-Internal-Api-Key", "secret");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("internal-service");
    }

    @Test
    void internalApiKeyFilterShouldIgnoreWrongOrPublicRequests() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter(new InternalApiProperties("X-Internal-Api-Key", "secret"));

        MockHttpServletRequest wrongRequest = new MockHttpServletRequest();
        wrongRequest.setRequestURI("/api/v1/internal/users/provision");
        wrongRequest.addHeader("X-Internal-Api-Key", "wrong");
        filter.doFilter(wrongRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest publicRequest = new MockHttpServletRequest();
        publicRequest.setRequestURI("/api/v1/users/me");
        filter.doFilter(publicRequest, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void internalApiKeyFilterShouldIgnoreMissingHeaderOnInternalRequest() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter(new InternalApiProperties("X-Internal-Api-Key", "secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/internal/users/provision");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private String signedToken(String subject, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key)
                .compact();
    }
}
