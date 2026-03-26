package com.example.insurance.service;

import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InsuranceClaimService {

    InsuranceClaimResponseDTO submitClaim(Long userId, InsuranceClaimRequestDTO request, List<MultipartFile> files);

    List<InsuranceClaimResponseDTO> getClaimsByUserId(Long userId);

    List<InsuranceClaimResponseDTO> getAllClaims();

    InsuranceClaimResponseDTO getClaimById(Long id, Long requesterUserId, boolean isAdmin);

    InsuranceClaimResponseDTO approveClaim(Long id, Double montant);

    InsuranceClaimResponseDTO rejectClaim(Long id);

    void deleteClaim(Long id);
}
