package com.example.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionCreateRequestDTO {

    private Long pharmacyId;

    @NotNull(message = "Patient id is required")
    private Long patientId;

    @NotBlank(message = "Patient name is required")
    private String patientName;

    @NotBlank(message = "Doctor name is required")
    private String doctorName;

    private String medicationName;

    private String dosage;

    private Integer quantity;

    private String instructions;

    private List<PrescriptionLineCreateRequestDTO> medicineLines;
}
