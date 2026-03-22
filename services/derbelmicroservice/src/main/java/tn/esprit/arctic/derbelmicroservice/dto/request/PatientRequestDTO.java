package tn.esprit.arctic.derbelmicroservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequestDTO {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String lastName;

    private LocalDate dateOfBirth;

    @Size(max = 10, message = "Le genre ne doit pas dépasser 10 caractères")
    private String gender;

    @Size(max = 5, message = "Le groupe sanguin ne doit pas dépasser 5 caractères")
    private String bloodType;

    @Size(max = 500, message = "Les allergies ne doivent pas dépasser 500 caractères")
    private String allergies;

    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    private String phone;
}
