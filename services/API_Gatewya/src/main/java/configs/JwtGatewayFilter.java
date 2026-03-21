package configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements WebFilter {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String path = exchange.getRequest().getURI().getPath();

        // Allow browser CORS preflight requests to pass.
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Allow auth endpoints without JWT
        if (path.startsWith("/api/auth")) {
            return chain.filter(exchange);
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret.getBytes())  // use your secret
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.get("userId", String.class);
            String role = claims.get("role", String.class);

            if (!StringUtils.hasText(role)) {
                String roles = claims.get("roles", String.class);
                if (StringUtils.hasText(roles)) {
                    String firstRole = roles.contains(",") ? roles.split(",")[0] : roles;
                    role = firstRole.startsWith("ROLE_") ? firstRole.substring(5) : firstRole;
                }
            }

            final String resolvedUserId = userId;
            final String resolvedRole = role;

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(builder -> {
                        if (StringUtils.hasText(resolvedUserId)) {
                            builder.header("userId", resolvedUserId);
                        }
                        if (StringUtils.hasText(resolvedRole)) {
                            builder.header("role", resolvedRole);
                        }
                    })
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}