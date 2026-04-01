package com.example.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionLineResponseDTO {

    private Long id;
    private String medicationName;
    private String dosage;
    private Integer quantity;
    private String instructions;
}
