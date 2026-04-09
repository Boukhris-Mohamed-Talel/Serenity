package com.example.pharmacy.controller;

import com.example.pharmacy.dto.PatientDefaultPharmacyRequestDTO;
import com.example.pharmacy.dto.PatientDefaultPharmacyResponseDTO;
import com.example.pharmacy.dto.PharmacyCandidateResponseDTO;
import com.example.pharmacy.service.PatientPharmacyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy/patient")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class PatientPharmacyController {

    private final PatientPharmacyService patientPharmacyService;

    @GetMapping("/default")
    public ResponseEntity<PatientDefaultPharmacyResponseDTO> getMyDefaultPharmacy() {
        return ResponseEntity.ok(patientPharmacyService.getMyDefaultPharmacy());
    }

    @PutMapping("/default")
    public ResponseEntity<PatientDefaultPharmacyResponseDTO> setMyDefaultPharmacy(
        @Valid @RequestBody PatientDefaultPharmacyRequestDTO request
    ) {
        return ResponseEntity.ok(patientPharmacyService.setMyDefaultPharmacy(request.getPharmacyId()));
    }

    @GetMapping("/pharmacies")
    public ResponseEntity<List<PharmacyCandidateResponseDTO>> listPharmacies(
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String governorate
    ) {
        return ResponseEntity.ok(patientPharmacyService.listPharmacies(city, governorate));
    }

    @GetMapping("/pharmacies/nearest")
    public ResponseEntity<List<PharmacyCandidateResponseDTO>> suggestNearest(
        @RequestParam Double latitude,
        @RequestParam Double longitude,
        @RequestParam(required = false) Double radiusKm
    ) {
        return ResponseEntity.ok(patientPharmacyService.suggestNearest(latitude, longitude, radiusKm));
    }
}
