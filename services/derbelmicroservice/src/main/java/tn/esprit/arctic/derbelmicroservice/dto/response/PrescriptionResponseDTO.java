package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponseDTO {

    private Long id;
    private List<PrescriptionMedicationResponseDTO> medications;
    private Long medicalRecordId;
    private Long patientId;
    private Long doctorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
