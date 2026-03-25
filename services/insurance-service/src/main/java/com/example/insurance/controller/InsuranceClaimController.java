package com.example.insurance.controller;

import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import com.example.insurance.security.InsuranceAuth;
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
            @RequestParam("description") String description,
            @RequestParam("amount") Double amount,
            @RequestParam("insuranceCompany") String insuranceCompany,
            @RequestParam("insuranceGrade") Double insuranceGrade,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Long userId = InsuranceAuth.requireUserId();
        InsuranceClaimRequestDTO request = InsuranceClaimRequestDTO.builder()
                .description(description)
                .amount(amount)
                .insuranceCompany(insuranceCompany)
                .insuranceGrade(insuranceGrade)
                .build();
        return ResponseEntity.ok(insuranceClaimService.submitClaim(userId, request, files));
    }

    @GetMapping("/claims/me")
    public ResponseEntity<List<InsuranceClaimResponseDTO>> getMyClaims() {
        Long userId = InsuranceAuth.requireUserId();
        return ResponseEntity.ok(insuranceClaimService.getClaimsByUserId(userId));
    }

    @GetMapping("/claims")
    public ResponseEntity<List<InsuranceClaimResponseDTO>> getAllClaims() {
        InsuranceAuth.requireAdmin();
        return ResponseEntity.ok(insuranceClaimService.getAllClaims());
    }

    @GetMapping("/claims/{id}")
    public ResponseEntity<InsuranceClaimResponseDTO> getClaimById(@PathVariable Long id) {
        Long userId = InsuranceAuth.requireUserId();
        boolean admin = InsuranceAuth.isAdmin();
        return ResponseEntity.ok(insuranceClaimService.getClaimById(id, userId, admin));
    }

    @PatchMapping("/claims/{id}/approve")
    public ResponseEntity<InsuranceClaimResponseDTO> approveClaim(
            @PathVariable Long id,
            @RequestParam Double montant) {
        InsuranceAuth.requireAdmin();
        return ResponseEntity.ok(insuranceClaimService.approveClaim(id, montant));
    }

    @PatchMapping("/claims/{id}/reject")
    public ResponseEntity<InsuranceClaimResponseDTO> rejectClaim(@PathVariable Long id) {
        InsuranceAuth.requireAdmin();
        return ResponseEntity.ok(insuranceClaimService.rejectClaim(id));
    }

    @DeleteMapping("/claims/{id}")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        InsuranceAuth.requireAdmin();
        insuranceClaimService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }
}
