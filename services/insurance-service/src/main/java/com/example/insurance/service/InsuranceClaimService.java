package com.example.insurance.service;

import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface InsuranceClaimService {

    InsuranceClaimResponseDTO submitClaim(Long userId, InsuranceClaimRequestDTO request, List<MultipartFile> files);

    List<InsuranceClaimResponseDTO> getClaimsByUserId(Long userId);

    List<InsuranceClaimResponseDTO> getClaimsByUserId(
            Long userId,
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    );

    List<InsuranceClaimResponseDTO> getAllClaims();

    List<InsuranceClaimResponseDTO> getAllClaims(
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    );

    InsuranceClaimResponseDTO getClaimById(Long id, Long requesterUserId, boolean isAdmin);

    InsuranceClaimResponseDTO approveClaim(Long id, Double montant);

    InsuranceClaimResponseDTO rejectClaim(Long id);

    void deleteClaim(Long id);
}
