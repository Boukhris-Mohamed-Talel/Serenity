package com.example.appointment.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches user emails from user-service (internal API) for reminder mail.
 */
@Slf4j
@Component
public class UserEmailClient {

    private final RestClient userServiceRestClient;

    @Value("${app.internal-api-key:}")
    private String internalApiKey;

    public UserEmailClient(@Qualifier("userServiceInternalRestClient") RestClient userServiceRestClient) {
        this.userServiceRestClient = userServiceRestClient;
    }

    public Map<Long, String> fetchEmailsByUserIds(List<Long> ids) {
        if (internalApiKey == null || internalApiKey.isBlank() || ids == null || ids.isEmpty()) {
            return Map.of();
        }
        try {
            List<UserEmailResponse> list = userServiceRestClient.post()
                    .uri("/api/internal/users/emails-by-ids")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Internal-Key", internalApiKey)
                    .body(new LookupIdsRequest(ids))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserEmailResponse>>() {});
            if (list == null || list.isEmpty()) {
                return Map.of();
            }
            Map<Long, String> map = new HashMap<>();
            for (UserEmailResponse r : list) {
                if (r.getId() != null && r.getEmail() != null && !r.getEmail().isBlank()) {
                    map.put(r.getId(), r.getEmail());
                }
            }
            return map;
        } catch (RestClientResponseException e) {
            log.warn("Internal email lookup failed: status={} body={}", e.getStatusCode().value(),
                    e.getResponseBodyAsString(StandardCharsets.UTF_8));
            return Map.of();
        } catch (Exception e) {
            log.warn("Could not fetch user emails: {}", e.getMessage());
            return Map.of();
        }
    }
}
