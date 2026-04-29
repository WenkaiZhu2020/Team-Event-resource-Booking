package com.teamresource.notification.infrastructure.security;

import com.teamresource.notification.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("stage2-layered-inactive")
public class JwtTokenService {

    private final SecretKey key;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secretBase64()));
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            return parse(token).getExpiration().toInstant().isAfter(java.time.Instant.now());
        } catch (Exception ex) {
            return false;
        }
    }
}
