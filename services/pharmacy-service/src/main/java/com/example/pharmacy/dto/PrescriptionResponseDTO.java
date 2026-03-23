package com.example.pharmacy.dto;

import com.example.pharmacy.entity.PrescriptionStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponseDTO {

    private Long id;
    private Long pharmacyId;
    private String pharmacyName;
    private Long doctorId;
    private Long patientId;
    private String doctorName;
    private String patientName;
    private Boolean assignedToPharmacy;
    private String assignmentMessage;
    private String medicationName;
    private String dosage;
    private Integer quantity;
    private String instructions;
    private List<PrescriptionLineResponseDTO> medicineLines;
    private PrescriptionStatus status;
    private String rejectionReason;
    private String readyAt;
    private String createdAt;
    private String updatedAt;
}
