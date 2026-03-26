package com.example.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorMedicineSuggestionItemDTO {

    private String medicineName;
    private String stockStatus;
    private Integer availableQuantity;
    private String guidanceMessage;
}
