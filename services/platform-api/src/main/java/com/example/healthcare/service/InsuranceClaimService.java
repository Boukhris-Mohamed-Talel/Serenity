package com.example.healthcare.service;

import com.example.healthcare.dto.InsuranceClaimRequestDTO;
import com.example.healthcare.dto.InsuranceClaimResponseDTO;
import com.example.healthcare.dto.RemboursementResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InsuranceClaimService {

    InsuranceClaimResponseDTO submitClaim(String email, InsuranceClaimRequestDTO request, List<MultipartFile> files);

    List<InsuranceClaimResponseDTO> getMyCllaims(String email);

    List<InsuranceClaimResponseDTO> getAllClaims();

    InsuranceClaimResponseDTO getClaimById(Long id);

    InsuranceClaimResponseDTO approveClaim(Long id, Double montant);

    InsuranceClaimResponseDTO rejectClaim(Long id);

    void deleteClaim(Long id);
}
