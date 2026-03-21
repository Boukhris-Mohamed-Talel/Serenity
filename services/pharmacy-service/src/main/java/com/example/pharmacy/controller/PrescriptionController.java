package com.example.pharmacy.controller;

import com.example.pharmacy.dto.PrescriptionCreateRequestDTO;
import com.example.pharmacy.dto.PrescriptionResponseDTO;
import com.example.pharmacy.dto.PrescriptionStatusUpdateRequestDTO;
import com.example.pharmacy.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponseDTO> createPrescription(@Valid @RequestBody PrescriptionCreateRequestDTO request) {
        return ResponseEntity.ok(prescriptionService.createPrescription(request));
    }

    @GetMapping("/inbox")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<List<PrescriptionResponseDTO>> getMyInbox() {
        return ResponseEntity.ok(prescriptionService.getMyInbox());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<PrescriptionResponseDTO>> getMineForPatient() {
        return ResponseEntity.ok(prescriptionService.getMyPatientPrescriptions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','PHARMACIST')")
    public ResponseEntity<PrescriptionResponseDTO> getPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(prescriptionService.getPrescription(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<PrescriptionResponseDTO> updatePrescriptionStatus(
        @PathVariable Long id,
        @Valid @RequestBody PrescriptionStatusUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(prescriptionService.updatePrescriptionStatus(id, request));
    }
}
