package com.example.pharmacy.controller;

import com.example.pharmacy.dto.PrescriptionCreateRequestDTO;
import com.example.pharmacy.dto.PrescriptionAlternativeResponseDTO;
import com.example.pharmacy.dto.PrescriptionPharmacyReassignRequestDTO;
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

    @GetMapping("/{id}/alternatives")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PrescriptionAlternativeResponseDTO> getAlternatives(
        @PathVariable Long id,
        @RequestParam Double latitude,
        @RequestParam Double longitude
    ) {
        return ResponseEntity.ok(prescriptionService.getPatientAlternatives(id, latitude, longitude));
    }

    @PutMapping("/{id}/pharmacy")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PrescriptionResponseDTO> reassignPharmacy(
        @PathVariable Long id,
        @Valid @RequestBody PrescriptionPharmacyReassignRequestDTO request
    ) {
        return ResponseEntity.ok(prescriptionService.reassignPatientPrescriptionPharmacy(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<PrescriptionResponseDTO> updatePrescriptionStatus(
        @PathVariable Long id,
        @Valid @RequestBody PrescriptionStatusUpdateRequestDTO request
    ) {
        return handleStatusUpdate(id, request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('PHARMACIST')")
    // Backward-compatible alias for clients still using POST instead of PATCH.
    public ResponseEntity<PrescriptionResponseDTO> updatePrescriptionStatusPost(
        @PathVariable Long id,
        @Valid @RequestBody PrescriptionStatusUpdateRequestDTO request
    ) {
        return handleStatusUpdate(id, request);
    }

    private ResponseEntity<PrescriptionResponseDTO> handleStatusUpdate(
        Long id,
        PrescriptionStatusUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(prescriptionService.updatePrescriptionStatus(id, request));
    }
}
