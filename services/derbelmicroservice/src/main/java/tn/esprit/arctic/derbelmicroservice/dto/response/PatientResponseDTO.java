package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodType;
    private String allergies;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
