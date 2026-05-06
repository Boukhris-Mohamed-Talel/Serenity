package com.example.marketplace.controller;

import com.example.marketplace.clients.MarketplaceOlistMlClient;
import com.example.marketplace.dto.OlistPythonRecommendResponse;
import com.example.marketplace.dto.OlistRecommendProxyRequest;
import com.example.marketplace.dto.OlistRecommendProxyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/articles/recommendations", "/api/marketplace/recommendations"})
@RequiredArgsConstructor
public class OlistMlRecommendationController {

    private final MarketplaceOlistMlClient mlClient;

    @PostMapping("/olist")
    public ResponseEntity<OlistRecommendProxyResponse> recommend(@Valid @RequestBody OlistRecommendProxyRequest request) {
        int topK = request.getTopK() == null ? 10 : request.getTopK();
        topK = Math.min(50, Math.max(1, topK));
        return mlClient
                .recommend(request.getCustomerUniqueId(), topK)
                .map(this::toProxy)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    private OlistRecommendProxyResponse toProxy(OlistPythonRecommendResponse py) {
        return OlistRecommendProxyResponse.builder()
                .customerUniqueId(py.getCustomerUniqueId())
                .coldStart(py.isColdStart())
                .items(
                        py.getItems() == null
                                ? java.util.List.of()
                                : py.getItems().stream()
                                        .map(
                                                i ->
                                                        OlistRecommendProxyResponse.OlistRecommendProxyItem.builder()
                                                                .productId(i.getProductId())
                                                                .score(i.getScore())
                                                                .category(i.getCategory() == null ? "" : i.getCategory())
                                                                .build())
                                        .collect(Collectors.toList()))
                .build();
    }
}
