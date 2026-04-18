package com.example.pharmacy.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class StockForecastAiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-service.base-url:http://localhost:8096}")
    private String aiServiceBaseUrl;

    @Value("${app.ai-service.internal-api-key:}")
    private String internalApiKey;

    public void runForecast(Long pharmacyId) {
        String url = aiServiceBaseUrl + "/internal/forecast/run";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalApiKey);

        Map<String, Object> body = pharmacyId == null ? Map.of() : Map.of("pharmacyId", pharmacyId);
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
    }
}
