package configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

@Component
public class JwtGatewayFilter implements WebFilter {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "";

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        if (path.startsWith("/api/auth")) {
            return chain.filter(exchange);
        }

        // Unauthenticated file access (portal attachment links, direct URLs to insurance-service uploads).
        if (path.startsWith("/api/files")) {
            return chain.filter(exchange);
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

            String userId = extractUserId(claims);
            if (StringUtils.hasText(userId)) {
                requestBuilder.header("X-User-Id", userId);
                requestBuilder.header("userId", userId);
            }

            String role = extractRole(claims);
            if (StringUtils.hasText(role)) {
                requestBuilder.header("role", role);
            }

            ServerHttpRequest newRequest = requestBuilder.build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (JwtException | IllegalArgumentException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private String extractUserId(Claims claims) {
        Object uid = claims.get("userId");
        if (uid instanceof Number number) {
            return String.valueOf(number.longValue());
        }
        if (uid instanceof String str && StringUtils.hasText(str)) {
            return str;
        }
        return null;
    }

    private String extractRole(Claims claims) {
        String role = claims.get("role", String.class);
        if (StringUtils.hasText(role)) {
            return role.startsWith("ROLE_") ? role.substring(5) : role;
        }

        String roles = claims.get("roles", String.class);
        if (StringUtils.hasText(roles)) {
            String first = roles.contains(",") ? roles.split(",")[0].trim() : roles.trim();
            return first.startsWith("ROLE_") ? first.substring(5) : first;
        }
        return null;
    }
}
