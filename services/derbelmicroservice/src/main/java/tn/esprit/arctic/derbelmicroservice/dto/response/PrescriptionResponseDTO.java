package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponseDTO {

    private Long id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String instructions;
    private int quantity;
    private String status;
    private Long medicalRecordId;
    private Long patientId;
    private Long doctorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
