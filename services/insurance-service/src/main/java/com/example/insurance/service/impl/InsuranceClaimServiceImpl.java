package com.example.insurance.service.impl;

import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import com.example.insurance.dto.RemboursementResponseDTO;
import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.Remboursement;
import com.example.insurance.repository.InsuranceClaimRepository;
import com.example.insurance.repository.RemboursementRepository;
import com.example.insurance.service.InsuranceClaimService;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceClaimServiceImpl implements InsuranceClaimService {

    private final InsuranceClaimRepository claimRepository;
    private final RemboursementRepository remboursementRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public InsuranceClaimResponseDTO submitClaim(Long userId, InsuranceClaimRequestDTO request, List<MultipartFile> files) {
        List<String> filePaths = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            filePaths = saveFiles(files, userId);
        }

        String externalRef = UUID.randomUUID().toString();

        InsuranceClaim claim = InsuranceClaim.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .status(ClaimStatus.PENDING)
                .externalRef(externalRef)
                .userId(userId)
                .filePaths(filePaths)
                .build();

        claimRepository.save(claim);
        return toResponseDTO(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getClaimsByUserId(Long userId) {
        return claimRepository.findByUserIdOrderByClaimDateDesc(userId)
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
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));
        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO approveClaim(Long id, Double montant) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));
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
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));
        claim.setStatus(ClaimStatus.REJECTED);
        claimRepository.save(claim);
        return toResponseDTO(claim);
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
        List<RemboursementResponseDTO> rembDtos = claim.getRemboursements() != null
                ? claim.getRemboursements().stream()
                .map(r -> RemboursementResponseDTO.builder()
                        .id(r.getId())
                        .montant(r.getMontant())
                        .date(r.getDate())
                        .statut(r.getStatut().name())
                        .claimId(claim.getId())
                        .build())
                .toList()
                : List.of();

        return InsuranceClaimResponseDTO.builder()
                .id(claim.getId())
                .description(claim.getDescription())
                .claimDate(claim.getClaimDate())
                .amount(claim.getAmount())
                .status(claim.getStatus().name())
                .externalRef(claim.getExternalRef())
                .filePaths(claim.getFilePaths())
                .userId(claim.getUserId())
                .remboursements(rembDtos)
                .build();
    }
}
