package com.teamresource.auth.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teamresource.auth.api.dto.AuthResponse;
import com.teamresource.auth.api.dto.TokenResponse;
import com.teamresource.auth.api.dto.UserResponse;
import com.teamresource.auth.config.OAuth2LoginProperties;
import com.teamresource.auth.config.SecurityConfig;
import com.teamresource.auth.domain.Role;
import com.teamresource.auth.service.AuthApplicationService;
import com.teamresource.common.security.JwtAuthenticationFilter;
import com.teamresource.common.security.JwtProperties;
import com.teamresource.common.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.Method;

class AuthSecurityAndOAuthTest {

    private static final String SECRET = "VEVBTV9SRVNPVVJDRV9NQU5BR0VNRU5UX0RFVl9TRUNSRVRfSFM1Nl8zMl9CWVRFUw==";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtServiceShouldGenerateParseAndValidateTokens() {
        JwtService service = new JwtService(new JwtProperties("issuer", SECRET, 60L));
        String token = service.generateAccessToken(UUID.randomUUID(), "user@example.com", Set.of(Role.USER, Role.ADMIN));

        assertThat(service.isValid(token)).isTrue();
        assertThat(service.parse(token).get("email")).isEqualTo("user@example.com");
        assertThat(service.extractRoles(service.parse(token))).containsExactlyInAnyOrder("USER", "ADMIN");
        assertThat(service.accessTokenTtlSeconds()).isEqualTo(3600L);
        assertThat(service.isValid("bad-token")).isFalse();
    }

    @Test
    void jwtAuthenticationFilterShouldAuthenticateRequest() throws Exception {
        JwtService service = new JwtService(new JwtProperties("issuer", SECRET, 60L));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service);
        String token = service.generateAccessToken(UUID.randomUUID(), "user@example.com", Set.of(Role.USER));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void jwtAuthenticationFilterShouldIgnoreMissingOrInvalidBearerHeader() throws Exception {
        JwtService service = new JwtService(new JwtProperties("issuer", SECRET, 60L));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service);

        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest();
        requestWithoutHeader.setRequestURI("/api/v1/auth/me");
        filter.doFilter(requestWithoutHeader, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest requestWithInvalidHeader = new MockHttpServletRequest();
        requestWithInvalidHeader.setRequestURI("/api/v1/auth/me");
        requestWithInvalidHeader.addHeader("Authorization", "Bearer bad-token");
        filter.doFilter(requestWithInvalidHeader, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        MockHttpServletRequest requestWithWrongScheme = new MockHttpServletRequest();
        requestWithWrongScheme.setRequestURI("/api/v1/auth/me");
        requestWithWrongScheme.addHeader("Authorization", "Basic abc123");
        filter.doFilter(requestWithWrongScheme, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void jwtAuthenticationFilterShouldKeepExistingRolePrefix() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .issuer("issuer")
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtService(new JwtProperties("issuer", SECRET, 60L)));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void jwtServiceShouldReturnEmptyRolesForNonListClaim() {
        JwtService service = new JwtService(new JwtProperties("issuer", SECRET, 60L));
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .issuer("issuer")
                .subject(UUID.randomUUID().toString())
                .claim("roles", "USER")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(service.extractRoles(service.parse(token))).isEmpty();
    }

    @Test
    void oauthFailureHandlerShouldRedirectWithError() throws Exception {
        OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler(
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, new BadCredentialsException("denied"));

        assertThat(response.getRedirectedUrl()).contains("http://localhost:5173/failure");
        assertThat(response.getRedirectedUrl()).contains("error=denied");
    }

    @Test
    void oauthSuccessHandlerShouldRedirectWithIssuedToken() throws Exception {
        StubAuthApplicationService authService = new StubAuthApplicationService();
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
                authService,
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure")
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                java.util.Map.of(
                        "email", "google@example.com",
                        "sub", "google-subject",
                        "name", "Google User",
                        "picture", "https://cdn.example/avatar.png",
                        "email_verified", true
                ),
                "email"
        );
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                oauth2User, null, List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).contains("http://localhost:5173/success");
        assertThat(response.getRedirectedUrl()).contains("accessToken=oauth-token");
        assertThat(response.getRedirectedUrl()).contains("roles=USER");
    }

    @Test
    void oauthSuccessHandlerShouldRejectMissingRequiredAttribute() {
        StubAuthApplicationService authService = new StubAuthApplicationService();
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
                authService,
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure")
        );
        OAuth2User oauth2User = new DefaultOAuth2User(List.of(), java.util.Map.of("email", "google@example.com"), "email");
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                oauth2User, null, List.of());

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), authentication))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void oauthSuccessHandlerShouldRejectBlankRequiredAttribute() {
        StubAuthApplicationService authService = new StubAuthApplicationService();
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
                authService,
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure")
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("email", "google@example.com", "sub", "   "),
                "email"
        );

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(oauth2User, null, List.of())
        )).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void oauthSuccessHandlerShouldUseFallbackAttributesAndSortRoles() throws Exception {
        StubAuthApplicationService authService = new StubAuthApplicationService();
        authService.response = new AuthResponse(
                new TokenResponse("oauth-token", "Bearer", 3600),
                new UserResponse(UUID.randomUUID(), "google@example.com", Set.of("USER", "ADMIN"), "ACTIVE")
        );
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
                authService,
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure")
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "email", "google@example.com",
                        "sub", "google-subject",
                        "name", "   ",
                        "picture", "   ",
                        "email_verified", false
                ),
                "email"
        );

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(oauth2User, null, List.of())
        );

        assertThat(authService.lastDisplayName).isEqualTo("google@example.com");
        assertThat(authService.lastAvatarUrl).isNull();
        assertThat(authService.lastEmailVerified).isFalse();
    }

    @Test
    void oauthSuccessHandlerShouldUseNullFallbackAttributes() throws Exception {
        StubAuthApplicationService authService = new StubAuthApplicationService();
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
                authService,
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure")
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "email", "google@example.com",
                        "sub", "google-subject",
                        "email_verified", true
                ),
                "email"
        );

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(oauth2User, null, List.of())
        );

        assertThat(authService.lastDisplayName).isEqualTo("google@example.com");
        assertThat(authService.lastAvatarUrl).isNull();
        assertThat(authService.lastEmailVerified).isTrue();
    }

    @Test
    void securityConfigShouldBuildFilterChainWithAndWithoutOauth2() throws Exception {
        SecurityConfig config = new SecurityConfig();
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(new JwtService(new JwtProperties("issuer", SECRET, 60L)));
        OAuth2AuthenticationSuccessHandler successHandler = new OAuth2AuthenticationSuccessHandler(
                new StubAuthApplicationService(),
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure")
        );
        OAuth2AuthenticationFailureHandler failureHandler = new OAuth2AuthenticationFailureHandler(
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure")
        );

        SecurityFilterChain disabledChain = invokeSecurityFilterChain(
                config,
                httpSecurity(),
                jwtFilter,
                successHandler,
                failureHandler,
                new OAuth2LoginProperties(false, "http://localhost:5173/success", "http://localhost:5173/failure")
        );
        SecurityFilterChain enabledChain = invokeSecurityFilterChain(
                config,
                httpSecurity(),
                jwtFilter,
                successHandler,
                failureHandler,
                new OAuth2LoginProperties(true, "http://localhost:5173/success", "http://localhost:5173/failure")
        );

        assertThat(disabledChain.getFilters()).anyMatch(filter -> filter instanceof AuthorizationFilter);
        assertThat(enabledChain.getFilters()).anyMatch(filter -> filter.getClass().getName().contains("OAuth2"));
    }

    private HttpSecurity httpSecurity() throws Exception {
        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.setServletContext(new MockServletContext());

        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        context.getBeanFactory().registerSingleton("objectPostProcessor", objectPostProcessor);
        context.getBeanFactory().registerSingleton(
                "mvcHandlerMappingIntrospector",
                new org.springframework.web.servlet.handler.HandlerMappingIntrospector()
        );
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .scope("openid", "profile", "email")
                .build();
        InMemoryClientRegistrationRepository clientRegistrationRepository =
                new InMemoryClientRegistrationRepository(registration);
        context.getBeanFactory().registerSingleton("clientRegistrationRepository", clientRegistrationRepository);
        context.getBeanFactory().registerSingleton(
                "authorizedClientService",
                new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
        );
        context.refresh();

        AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
        HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, Map.of());
        http.setSharedObject(org.springframework.context.ApplicationContext.class, context);
        http.setSharedObject(org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.class, new org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter());
        http.setSharedObject(org.springframework.web.accept.ContentNegotiationStrategy.class, new HeaderContentNegotiationStrategy());
        return http;
    }

    private SecurityFilterChain invokeSecurityFilterChain(
            SecurityConfig config,
            HttpSecurity httpSecurity,
            JwtAuthenticationFilter jwtFilter,
            OAuth2AuthenticationSuccessHandler successHandler,
            OAuth2AuthenticationFailureHandler failureHandler,
            OAuth2LoginProperties properties
    ) throws Exception {
        Method method = SecurityConfig.class.getDeclaredMethod(
                "securityFilterChain",
                HttpSecurity.class,
                JwtAuthenticationFilter.class,
                OAuth2AuthenticationSuccessHandler.class,
                OAuth2AuthenticationFailureHandler.class,
                OAuth2LoginProperties.class
        );
        method.setAccessible(true);
        return (SecurityFilterChain) method.invoke(config, httpSecurity, jwtFilter, successHandler, failureHandler, properties);
    }

    private static final class StubAuthApplicationService extends AuthApplicationService {
        private AuthResponse response = new AuthResponse(
                new TokenResponse("oauth-token", "Bearer", 3600),
                new UserResponse(UUID.randomUUID(), "google@example.com", Set.of("USER"), "ACTIVE")
        );
        private String lastDisplayName;
        private String lastAvatarUrl;
        private boolean lastEmailVerified;

        private StubAuthApplicationService() {
            super(null, null, null, null);
        }

        @Override
        public AuthResponse loginOrRegisterGoogle(String email, String googleSubject, String displayName, String avatarUrl, boolean emailVerified) {
            this.lastDisplayName = displayName;
            this.lastAvatarUrl = avatarUrl;
            this.lastEmailVerified = emailVerified;
            return new AuthResponse(
                    response.tokens(),
                    new UserResponse(response.user().userId(), email, response.user().roles(), response.user().status())
            );
        }
    }
}
