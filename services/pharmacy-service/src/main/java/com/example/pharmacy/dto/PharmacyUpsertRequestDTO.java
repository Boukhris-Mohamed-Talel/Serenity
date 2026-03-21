package com.example.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyUpsertRequestDTO {

    @NotBlank(message = "Pharmacy name is required")
    private String name;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    private String phone;
    private String openingHours;
    private String addressLine;
    private String city;
    private String governorate;
    private Double latitude;
    private Double longitude;
    private Boolean supportsEmergency;
}
