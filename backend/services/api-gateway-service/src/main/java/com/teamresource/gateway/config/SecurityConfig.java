package com.teamresource.gateway.config;

import com.teamresource.gateway.security.JsonAccessDeniedHandler;
import com.teamresource.gateway.security.JsonAuthenticationEntryPoint;
import com.teamresource.gateway.security.JwtRoleAuthenticationConverter;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            GatewayRouteProperties routeProperties,
            JwtRoleAuthenticationConverter jwtRoleAuthenticationConverter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler
    ) {
        String[] publicPaths = routeProperties.publicPaths().toArray(String[]::new);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .exceptionHandling(spec -> spec
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(publicPaths).permitAll()
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleAuthenticationConverter)))
                .build();
    }

    @Bean
    ReactiveJwtDecoder gatewayJwtDecoder(@Value("${app.security.jwt.secret-base64}") String secretBase64) {
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(secretBase64), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
