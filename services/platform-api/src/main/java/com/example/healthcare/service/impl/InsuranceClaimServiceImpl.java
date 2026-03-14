package com.example.healthcare.service.impl;

import com.example.healthcare.dto.InsuranceClaimRequestDTO;
import com.example.healthcare.dto.InsuranceClaimResponseDTO;
import com.example.healthcare.dto.RemboursementResponseDTO;
import com.example.healthcare.entity.*;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.repository.InsuranceClaimRepository;
import com.example.healthcare.repository.RemboursementRepository;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.service.InsuranceClaimService;
import com.example.healthcare.service.InsurancePortalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceClaimServiceImpl implements InsuranceClaimService {

    private final InsuranceClaimRepository claimRepository;
    private final RemboursementRepository remboursementRepository;
    private final UserRepository userRepository;
    private final InsurancePortalClient insurancePortalClient;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final Map<Integer, Double> GRADE_PERCENTAGES = Map.of(
            1, 0.10,
            2, 0.12,
            3, 0.18,
            4, 0.25,
            5, 0.45
    );

    @Override
    public InsuranceClaimResponseDTO submitClaim(String email, InsuranceClaimRequestDTO request, List<MultipartFile> files) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        List<String> filePaths = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            filePaths = saveFiles(files, user.getId());
        }

        double percentage = GRADE_PERCENTAGES.getOrDefault(request.getInsuranceGrade(), 0.0);
        double reimbursementAmount = Math.round(request.getAmount() * percentage * 100.0) / 100.0;

        String externalRef = UUID.randomUUID().toString();

        InsuranceClaim claim = InsuranceClaim.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .insuranceCompany(request.getInsuranceCompany())
                .insuranceGrade(request.getInsuranceGrade())
                .reimbursementAmount(reimbursementAmount)
                .status(ClaimStatus.PENDING)
                .externalRef(externalRef)
                .filePaths(filePaths)
                .user(user)
                .build();

        claimRepository.save(claim);

        String patientName = user.getFirstName() + " " + user.getLastName();
        insurancePortalClient.forwardClaim(externalRef, patientName, request.getDescription(),
                reimbursementAmount, request.getInsuranceCompany(), request.getInsuranceGrade());

        return toResponseDTO(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getMyCllaims(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return claimRepository.findByUserIdOrderByClaimDateDesc(user.getId())
                .stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getAllClaims() {
        return claimRepository.findAllByOrderByClaimDateDesc()
                .stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceClaimResponseDTO getClaimById(Long id) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceClaim", "id", id));
        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO approveClaim(Long id, Double montant) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceClaim", "id", id));

        claim.setStatus(ClaimStatus.APPROVED);

        Remboursement remboursement = Remboursement.builder()
                .montant(montant)
                .statut(ClaimStatus.APPROVED)
                .insuranceClaim(claim)
                .build();
        remboursementRepository.save(remboursement);

        claimRepository.save(claim);
        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO rejectClaim(Long id) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceClaim", "id", id));

        claim.setStatus(ClaimStatus.REJECTED);
        claimRepository.save(claim);
        return toResponseDTO(claim);
    }

    @Override
    public void deleteClaim(Long id) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsuranceClaim", "id", id));

        if (claim.getStatus() != ClaimStatus.REJECTED) {
            throw new IllegalStateException("Only rejected claims can be deleted");
        }

        claimRepository.delete(claim);
    }

    private List<String> saveFiles(List<MultipartFile> files, Long userId) {
        List<String> paths = new ArrayList<>();
        try {
            Path uploadPath = Paths.get(uploadDir, "claims", userId.toString());
            Files.createDirectories(uploadPath);

            for (MultipartFile file : files) {
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(filename);
                file.transferTo(filePath.toAbsolutePath().toFile());
                paths.add(filePath.toString().replace("\\", "/"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload files", e);
        }
        return paths;
    }

    private InsuranceClaimResponseDTO toResponseDTO(InsuranceClaim claim) {
        List<RemboursementResponseDTO> remboursementDTOs = claim.getRemboursements() != null
                ? claim.getRemboursements().stream().map(r -> RemboursementResponseDTO.builder()
                    .id(r.getId())
                    .montant(r.getMontant())
                    .date(r.getDate())
                    .statut(r.getStatut().name())
                    .claimId(claim.getId())
                    .build()).toList()
                : List.of();

        return InsuranceClaimResponseDTO.builder()
                .id(claim.getId())
                .description(claim.getDescription())
                .claimDate(claim.getClaimDate())
                .amount(claim.getAmount())
                .insuranceCompany(claim.getInsuranceCompany())
                .insuranceGrade(claim.getInsuranceGrade())
                .reimbursementAmount(claim.getReimbursementAmount())
                .status(claim.getStatus().name())
                .externalRef(claim.getExternalRef())
                .filePaths(claim.getFilePaths())
                .userId(claim.getUser().getId())
                .userFullName(claim.getUser().getFirstName() + " " + claim.getUser().getLastName())
                .remboursements(remboursementDTOs)
                .build();
    }
}
