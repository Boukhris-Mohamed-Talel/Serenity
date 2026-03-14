package com.example.healthcare.controller;

import com.example.healthcare.dto.InsuranceClaimRequestDTO;
import com.example.healthcare.dto.InsuranceClaimResponseDTO;
import com.example.healthcare.service.InsuranceClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
            Authentication authentication,
            @RequestParam("description") String description,
            @RequestParam("amount") Double amount,
            @RequestParam("insuranceCompany") String insuranceCompany,
            @RequestParam("insuranceGrade") Integer insuranceGrade,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        InsuranceClaimRequestDTO request = new InsuranceClaimRequestDTO(description, amount, insuranceCompany, insuranceGrade);
        return ResponseEntity.ok(insuranceClaimService.submitClaim(authentication.getName(), request, files));
    }

    @GetMapping("/claims/me")
    public ResponseEntity<List<InsuranceClaimResponseDTO>> getMyClaims(Authentication authentication) {
        return ResponseEntity.ok(insuranceClaimService.getMyCllaims(authentication.getName()));
    }

    @GetMapping("/claims")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InsuranceClaimResponseDTO>> getAllClaims() {
        return ResponseEntity.ok(insuranceClaimService.getAllClaims());
    }

    @GetMapping("/claims/{id}")
    public ResponseEntity<InsuranceClaimResponseDTO> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(insuranceClaimService.getClaimById(id));
    }

    @PatchMapping("/claims/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceClaimResponseDTO> approveClaim(
            @PathVariable Long id,
            @RequestParam Double montant) {
        return ResponseEntity.ok(insuranceClaimService.approveClaim(id, montant));
    }

    @PatchMapping("/claims/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsuranceClaimResponseDTO> rejectClaim(@PathVariable Long id) {
        return ResponseEntity.ok(insuranceClaimService.rejectClaim(id));
    }

    @DeleteMapping("/claims/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        insuranceClaimService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }
}
