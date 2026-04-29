package com.serenity.monitoring.config;

import com.serenity.monitoring.security.jwt.JwtAuthenticationEntryPoint;
import com.serenity.monitoring.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigCoverageTest {

    @Test
    void monitoringAiProperties_gettersAndSetters() {
        MonitoringAiProperties props = new MonitoringAiProperties();
        props.setEnabled(false);
        props.setUrl("http://localhost:9999");

        assertEquals(false, props.isEnabled());
        assertEquals("http://localhost:9999", props.getUrl());
    }

    @Test
    void monitoringAiRestClientConfig_buildsRestClientWithBaseUrl() {
        MonitoringAiRestClientConfig config = new MonitoringAiRestClientConfig();
        MonitoringAiProperties props = new MonitoringAiProperties();
        props.setUrl("http://localhost:5150");
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl("http://localhost:5150")).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        RestClient result = config.monitoringAiRestClient(builder, props);

        assertNotNull(result);
    }

    @Test
    void securityConfig_buildsCorsConfigurationSource() {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthenticationFilter.class), mock(JwtAuthenticationEntryPoint.class));
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:4200, http://localhost:3000");
        ReflectionTestUtils.setField(config, "allowCredentials", true);

        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/monitoring/test");

        assertTrue(source instanceof UrlBasedCorsConfigurationSource);
        assertNotNull(((UrlBasedCorsConfigurationSource) source).getCorsConfiguration(request));
    }
}
