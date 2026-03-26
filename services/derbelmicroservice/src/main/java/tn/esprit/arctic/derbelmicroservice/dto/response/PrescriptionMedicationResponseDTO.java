package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionMedicationResponseDTO {
    private String medicationName;
    private String dosage;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String instructions;
    private int quantity;
    private String status;
}
