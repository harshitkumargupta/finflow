package com.finflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            Environment environment) {

        boolean test = java.util.Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (!test && (secret == null || secret.isBlank())) {
            throw new IllegalStateException("FINFLOW_JWT_SECRET must be configured outside the test profile");
        }
        if (!test && secret.length() < 32) {
            throw new IllegalStateException("FINFLOW_JWT_SECRET is too short");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("FINFLOW_JWT_SECRET must be configured for the test profile");
        }

        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );

        this.expiration = expiration;
    }

    public String generateToken(String email) {

        Date now = new Date();

        Date expiry =
                new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    public boolean isTokenValid(String token) {

        try {

            Claims claims = extractAllClaims(token);

            return claims.getExpiration()
                    .after(new Date());

        } catch (Exception e) {

            return false;
        }
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
