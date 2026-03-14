package com.example.insurance.controller;

import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import com.example.insurance.service.InsuranceClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/insurance")
@RequiredArgsConstructor
public class InsuranceClaimController {

    private final InsuranceClaimService insuranceClaimService;

    @PostMapping(value = "/claims", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InsuranceClaimResponseDTO> submitClaim(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @RequestParam("description") String description,
            @RequestParam("amount") Double amount,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        InsuranceClaimRequestDTO request = new InsuranceClaimRequestDTO(description, amount);
        return ResponseEntity.ok(insuranceClaimService.submitClaim(userId, request, files));
    }

    @GetMapping("/claims/me")
    public ResponseEntity<List<InsuranceClaimResponseDTO>> getMyClaims(
            @RequestHeader(value = "X-User-Id", required = true) Long userId) {
        return ResponseEntity.ok(insuranceClaimService.getClaimsByUserId(userId));
    }

    @GetMapping("/claims")
    public ResponseEntity<List<InsuranceClaimResponseDTO>> getAllClaims() {
        return ResponseEntity.ok(insuranceClaimService.getAllClaims());
    }

    @GetMapping("/claims/{id}")
    public ResponseEntity<InsuranceClaimResponseDTO> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(insuranceClaimService.getClaimById(id));
    }

    @PatchMapping("/claims/{id}/approve")
    public ResponseEntity<InsuranceClaimResponseDTO> approveClaim(
            @PathVariable Long id,
            @RequestParam Double montant) {
        return ResponseEntity.ok(insuranceClaimService.approveClaim(id, montant));
    }

    @PatchMapping("/claims/{id}/reject")
    public ResponseEntity<InsuranceClaimResponseDTO> rejectClaim(@PathVariable Long id) {
        return ResponseEntity.ok(insuranceClaimService.rejectClaim(id));
    }
}
