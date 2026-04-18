package com.example.pharmacy.dto;

import com.example.pharmacy.entity.StockoutRisk;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StockReorderSuggestionDTO {
    String medicineName;
    Integer currentQty;
    Double demand14;
    Integer suggestedReorderQty;
    StockoutRisk stockoutRisk;
    String generatedAt;
}
