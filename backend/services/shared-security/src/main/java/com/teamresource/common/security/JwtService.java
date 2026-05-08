package com.teamresource.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secretBase64()));
    }

    public String generateAccessToken(UUID userId, String email, Collection<?> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(accessTokenTtlSeconds());

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles.stream().map(this::roleName).toList())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
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

    public long accessTokenTtlSeconds() {
        return properties.requiredAccessTokenMinutes() * 60;
    }

    public List<String> extractRoles(Claims claims) {
        Object claim = claims.get("roles");
        if (claim instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private String roleName(Object role) {
        if (role instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return String.valueOf(role);
    }
}
