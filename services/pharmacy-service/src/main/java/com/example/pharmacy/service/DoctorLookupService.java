package com.example.pharmacy.service;

import com.example.pharmacy.dto.DoctorMedicineSuggestionResponseDTO;
import com.example.pharmacy.dto.DoctorPatientSuggestionResponseDTO;

public interface DoctorLookupService {
    DoctorPatientSuggestionResponseDTO suggestPatients(String query);
    DoctorMedicineSuggestionResponseDTO suggestMedicines(Long patientId, String query);
}
