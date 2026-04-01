package com.example.pharmacy.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorPatientSuggestionResponseDTO {

    private List<DoctorPatientSuggestionItemDTO> suggestions;
}
