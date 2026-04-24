package com.teamresource.event.infra.security;

import com.teamresource.event.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secretBase64()));
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
            Claims claims = parse(token);
            return claims.getExpiration().toInstant().isAfter(Instant.now());
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
