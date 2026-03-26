package tn.esprit.arctic.derbelmicroservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionMedicationRequestDTO {

    @NotBlank(message = "Le nom du médicament est obligatoire")
    @Size(max = 100, message = "Le nom du médicament ne doit pas dépasser 100 caractères")
    private String medicationName;

    @NotBlank(message = "Le dosage est obligatoire")
    @Size(max = 50, message = "Le dosage ne doit pas dépasser 50 caractères")
    private String dosage;

    @NotBlank(message = "La fréquence est obligatoire")
    @Size(max = 50, message = "La fréquence ne doit pas dépasser 50 caractères")
    private String frequency;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500, message = "Les instructions ne doivent pas dépasser 500 caractères")
    private String instructions;

    @Min(value = 1, message = "La quantité doit être au minimum 1")
    private int quantity;

    @NotBlank(message = "Le statut est obligatoire (ACTIVE, INACTIVE)")
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Le statut doit être ACTIVE ou INACTIVE")
    private String status;
}
