package com.teamresource.booking.infrastructure.security;

import com.teamresource.booking.infrastructure.config.InternalApiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Profile("stage2-layered-inactive")
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private final InternalApiProperties properties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public InternalApiKeyFilter(InternalApiProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (antPathMatcher.match("/api/v1/internal/**", request.getRequestURI())) {
            String apiKey = request.getHeader(properties.keyHeaderName());
            if (properties.keyValue().equals(apiKey)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "internal-service",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
