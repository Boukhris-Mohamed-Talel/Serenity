package configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class GatewayCorsConfig {

    // Intentionally left blank:
    // CORS is handled by downstream services (user-service + insurance-service)
    // to avoid duplicate Access-Control-Allow-Origin headers being added by both gateway and services.
}
