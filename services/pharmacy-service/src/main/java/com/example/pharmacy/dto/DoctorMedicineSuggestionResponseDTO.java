package com.example.pharmacy.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorMedicineSuggestionResponseDTO {

    private Long patientId;
    private Boolean hasDefaultPharmacy;
    private Long pharmacyId;
    private String pharmacyName;
    private String guidanceMessage;
    private List<DoctorMedicineSuggestionItemDTO> suggestions;
}
