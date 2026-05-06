package com.example.marketplace.clients;

import com.example.marketplace.dto.OlistPythonRecommendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketplaceOlistMlClient {

    private final RestTemplate restTemplate;

    @Value("${app.ml.olist-base-url:}")
    private String olistBaseUrl;

    public Optional<OlistPythonRecommendResponse> recommend(String customerUniqueId, int topK) {
        if (olistBaseUrl == null || olistBaseUrl.isBlank()) {
            return Optional.empty();
        }
        String base = olistBaseUrl.endsWith("/") ? olistBaseUrl.substring(0, olistBaseUrl.length() - 1) : olistBaseUrl;
        String url = base + "/v1/recommend";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body =
                Map.of(
                        "customer_unique_id", customerUniqueId,
                        "top_k", topK,
                        "mask_purchased", true);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            OlistPythonRecommendResponse response =
                    restTemplate.postForObject(url, entity, OlistPythonRecommendResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException ex) {
            log.warn("Olist ML service call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
