package com.serenity.monitoring.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    @Test
    void constructor_throwsWhenSecretMissing() {
        Environment env = mock(Environment.class);
        when(env.getProperty("app.jwt.secret")).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(env));
    }

    @Test
    void validateAndExtractEmail_workForValidToken() {
        byte[] raw = "12345678901234567890123456789012".getBytes();
        String secret = Base64.getEncoder().encodeToString(raw);
        Environment env = mock(Environment.class);
        when(env.getProperty("app.jwt.secret")).thenReturn(secret);
        JwtTokenProvider provider = new JwtTokenProvider(env);

        String token = Jwts.builder()
                .subject("doctor@test.com")
                .issuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .compact();

        assertTrue(provider.validateToken(token));
        assertEquals("doctor@test.com", provider.getEmailFromToken(token));
    }

    @Test
    void validateToken_returnsFalseForInvalidToken() {
        byte[] raw = "12345678901234567890123456789012".getBytes();
        String secret = Base64.getEncoder().encodeToString(raw);
        Environment env = mock(Environment.class);
        when(env.getProperty("app.jwt.secret")).thenReturn(secret);
        JwtTokenProvider provider = new JwtTokenProvider(env);

        assertFalse(provider.validateToken("not-a-token"));
    }
}
