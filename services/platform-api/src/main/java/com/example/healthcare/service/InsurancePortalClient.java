package com.example.healthcare.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class InsurancePortalClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.insurance.api-url:http://localhost:3000/api}")
    private String insuranceApiUrl;

    public void forwardClaim(String externalRef, String patientName, String description,
                             Double amount, String insuranceCompany, Integer insuranceGrade) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("ref", externalRef);
            body.put("patientName", patientName);
            body.put("description", description);
            body.put("amount", amount);
            body.put("insuranceCompany", insuranceCompany != null ? insuranceCompany : "N/A");
            body.put("insuranceGrade", insuranceGrade != null ? insuranceGrade : 0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(insuranceApiUrl + "/claims", request, String.class);
            log.info("Forwarded claim {} to insurance portal", externalRef);
        } catch (Exception e) {
            log.warn("Failed to forward claim {} to insurance portal: {}", externalRef, e.getMessage());
        }
    }

    public ClaimStatusResponse pollClaimStatus(String externalRef) {
        try {
            String url = insuranceApiUrl + "/claims/" + externalRef + "/status";
            return restTemplate.getForObject(url, ClaimStatusResponse.class);
        } catch (Exception e) {
            log.warn("Failed to poll status for claim {}: {}", externalRef, e.getMessage());
            return null;
        }
    }

    @Getter
    @Setter
    public static class ClaimStatusResponse {
        private String ref;
        private String status;
        private Double reimbursementAmount;
    }
}
