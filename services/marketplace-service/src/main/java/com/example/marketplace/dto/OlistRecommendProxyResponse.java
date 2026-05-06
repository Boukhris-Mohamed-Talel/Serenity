package com.example.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OlistRecommendProxyResponse {
    private String customerUniqueId;
    private boolean coldStart;
    private List<OlistRecommendProxyItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OlistRecommendProxyItem {
        private String productId;
        private double score;
        private String category;
    }
}
