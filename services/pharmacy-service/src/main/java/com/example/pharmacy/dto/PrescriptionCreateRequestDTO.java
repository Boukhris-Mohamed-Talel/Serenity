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

    /**
     * Legacy single-line field kept for compatibility with older clients.
     * New clients should use medicineLines instead.
     */
    @Deprecated
    private String medicationName;

    /**
     * Legacy single-line field kept for compatibility with older clients.
     * New clients should use medicineLines instead.
     */
    @Deprecated
    private String dosage;

    /**
     * Legacy single-line field kept for compatibility with older clients.
     * New clients should use medicineLines instead.
     */
    @Deprecated
    private Integer quantity;

    /**
     * Legacy single-line field kept for compatibility with older clients.
     * New clients should use medicineLines instead.
     */
    @Deprecated
    private String instructions;

    private List<PrescriptionLineCreateRequestDTO> medicineLines;
}
