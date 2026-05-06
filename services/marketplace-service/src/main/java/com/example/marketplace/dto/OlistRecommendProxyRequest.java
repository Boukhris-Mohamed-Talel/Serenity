package com.example.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OlistRecommendProxyRequest {

    @NotBlank
    @Size(max = 128)
    private String customerUniqueId;

    @Positive
    @Max(50)
    private Integer topK;
}
