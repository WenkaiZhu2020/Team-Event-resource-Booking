package com.teamresource.booking.infrastructure.config;

import com.teamresource.booking.infrastructure.security.InternalApiKeyFilter;
import com.teamresource.booking.infrastructure.security.JwtAuthenticationFilter;
import com.teamresource.booking.infrastructure.security.JsonAuthEntryPoint;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@Profile("source-architecture")
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            InternalApiKeyFilter internalApiKeyFilter,
            JsonAuthEntryPoint entryPoint
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/internal/**").hasRole("INTERNAL_SERVICE")
                        .requestMatchers("/api/v1/bookings/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
