package com.example.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorPatientSuggestionItemDTO {

    private Long patientId;
    private String displayName;
    private String profilePictureUrl;
}
