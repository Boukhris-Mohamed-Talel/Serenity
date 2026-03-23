package com.example.pharmacy.controller;

import com.example.pharmacy.dto.DoctorMedicineSuggestionResponseDTO;
import com.example.pharmacy.service.DoctorLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pharmacy/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorLookupController {

    private final DoctorLookupService doctorLookupService;

    @GetMapping("/medicine-suggestions")
    public ResponseEntity<DoctorMedicineSuggestionResponseDTO> suggestMedicines(
        @RequestParam Long patientId,
        @RequestParam String query
    ) {
        return ResponseEntity.ok(doctorLookupService.suggestMedicines(patientId, query));
    }
}
