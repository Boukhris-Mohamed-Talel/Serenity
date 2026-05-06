package com.example.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** JSON shape returned by the Python FastAPI service. */
@Data
public class OlistPythonRecommendResponse {

    @JsonProperty("customer_unique_id")
    private String customerUniqueId;

    private List<OlistPythonRecommendItem> items;

    @JsonProperty("cold_start")
    private boolean coldStart;

    @Data
    public static class OlistPythonRecommendItem {
        @JsonProperty("product_id")
        private String productId;

        private double score;
        private String category;
    }
}
