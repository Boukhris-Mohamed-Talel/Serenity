package com.example.pharmacy.service;

import com.example.pharmacy.dto.PrescriptionAlternativeResponseDTO;
import com.example.pharmacy.dto.PrescriptionPharmacyReassignRequestDTO;
import com.example.pharmacy.dto.PrescriptionResponseDTO;
import com.example.pharmacy.dto.PrescriptionStatusUpdateRequestDTO;

import java.util.List;

public interface PrescriptionService {
    List<PrescriptionResponseDTO> getMyInbox();
    List<PrescriptionResponseDTO> getMyPatientPrescriptions();
    PrescriptionResponseDTO getPrescription(Long id);
    PrescriptionAlternativeResponseDTO getPatientAlternatives(Long id, Double latitude, Double longitude);
    PrescriptionResponseDTO reassignPatientPrescriptionPharmacy(Long id, PrescriptionPharmacyReassignRequestDTO request);
    PrescriptionResponseDTO updatePrescriptionStatus(Long id, PrescriptionStatusUpdateRequestDTO request);
}
