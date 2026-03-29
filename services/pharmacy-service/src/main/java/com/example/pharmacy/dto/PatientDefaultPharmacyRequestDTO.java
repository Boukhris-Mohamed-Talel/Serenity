package com.example.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDefaultPharmacyRequestDTO {

    @NotNull(message = "Pharmacy id is required")
    private Long pharmacyId;
}
