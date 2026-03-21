package com.example.pharmacy.service;

import com.example.pharmacy.dto.PrescriptionCreateRequestDTO;
import com.example.pharmacy.dto.PrescriptionResponseDTO;
import com.example.pharmacy.dto.PrescriptionStatusUpdateRequestDTO;

import java.util.List;

public interface PrescriptionService {
    PrescriptionResponseDTO createPrescription(PrescriptionCreateRequestDTO request);
    List<PrescriptionResponseDTO> getMyInbox();
    List<PrescriptionResponseDTO> getMyPatientPrescriptions();
    PrescriptionResponseDTO getPrescription(Long id);
    PrescriptionResponseDTO updatePrescriptionStatus(Long id, PrescriptionStatusUpdateRequestDTO request);
}
