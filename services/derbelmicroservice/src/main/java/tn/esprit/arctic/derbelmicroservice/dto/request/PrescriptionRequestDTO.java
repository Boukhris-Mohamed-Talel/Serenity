package tn.esprit.arctic.derbelmicroservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionRequestDTO {

    @NotEmpty(message = "Au moins un médicament est obligatoire")
    private List<@Valid PrescriptionMedicationRequestDTO> medications;

    @NotNull(message = "L'ID du dossier médical est obligatoire")
    private Long medicalRecordId;

    @NotNull(message = "L'ID du patient est obligatoire")
    private Long patientId;

    private Long doctorId;
}
