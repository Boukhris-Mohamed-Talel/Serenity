package com.example.pharmacy.service;

import com.example.pharmacy.dto.DoctorMedicineSuggestionResponseDTO;

public interface DoctorLookupService {
    DoctorMedicineSuggestionResponseDTO suggestMedicines(Long patientId, String query);
}
