package com.example.healthcare.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        String email = authentication.getName();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        // Extract userId from CustomUserDetails if available
        Long userId = null;
        if (authentication.getPrincipal() instanceof com.example.healthcare.security.userdetails.CustomUserDetails) {
            userId = ((com.example.healthcare.security.userdetails.CustomUserDetails) authentication.getPrincipal()).getId();
        }
        
        return generateToken(email, roles, userId);
    }

    /**
     * Deprecated: use generateToken(String email, String roles, Long userId) instead.
     * This method is kept for backward compatibility.
     */
    @Deprecated
    public String generateToken(String email, String roles) {
        return generateToken(email, roles, null);
    }

    /**
     * Generates token with userId claim for microservice communication.
     */
    public String generateToken(String email, String roles, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .subject(email)
                .claim("roles", roles);
        
        // Include userId if available (required by monitoring-service and other microservices)
        if (userId != null) {
            builder.claim("userId", userId);
        }
        
        return builder
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
