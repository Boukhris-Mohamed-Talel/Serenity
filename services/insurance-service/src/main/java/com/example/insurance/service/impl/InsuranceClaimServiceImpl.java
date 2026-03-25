package com.example.insurance.service.impl;

import com.example.insurance.dto.InsuranceClaimRequestDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import com.example.insurance.dto.RemboursementResponseDTO;
import com.example.insurance.integration.InsurancePortalClient;
import com.example.insurance.integration.PortalSubmitClaimRequest;
import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.NotificationType;
import com.example.insurance.entity.Remboursement;
import com.example.insurance.repository.InsuranceClaimRepository;
import com.example.insurance.repository.RemboursementRepository;
import com.example.insurance.service.InsuranceClaimService;
import com.example.insurance.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceClaimServiceImpl implements InsuranceClaimService {

    private final InsuranceClaimRepository claimRepository;
    private final RemboursementRepository remboursementRepository;
    private final InsurancePortalClient insurancePortalClient;
    private final NotificationService notificationService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    @Value("${app.public-base-url:http://localhost:8090}")
    private String publicBaseUrl;

    @Override
    public InsuranceClaimResponseDTO submitClaim(Long userId, InsuranceClaimRequestDTO request, List<MultipartFile> files) {
        List<String> filePaths = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            filePaths = saveFiles(files, userId);
        }

        String externalRef = UUID.randomUUID().toString();

        // Used to populate the DB column `reimbursement_amount` (non-null in schema)
        Double reimbursementAmount = calculateReimbursement(request.getAmount(), request.getInsuranceGrade());

        InsuranceClaim claim = InsuranceClaim.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .reimbursementAmount(reimbursementAmount)
                .insuranceCompany(request.getInsuranceCompany())
                .insuranceGrade(request.getInsuranceGrade())
                .reason(null)
                .status(ClaimStatus.PENDING)
                .externalRef(externalRef)
                .userId(userId)
                .filePaths(filePaths)
                .build();

        claimRepository.save(claim);

        // Fire-and-forget: external provider may accept/reject asynchronously.
        // Our scheduler will poll portal status and update this claim.
        PortalSubmitClaimRequest portalReq = new PortalSubmitClaimRequest(
                externalRef,
                String.valueOf(userId), // portal displays patientName; we only have userId here
                request.getDescription(),
                request.getAmount(),
                reimbursementAmount,
                request.getInsuranceCompany(),
                request.getInsuranceGrade(),
                buildAttachmentUrls(filePaths)
        );
        insurancePortalClient.submitClaim(portalReq);

        notificationService.createNotification(
                userId,
                claim.getId(),
                NotificationType.CLAIM_SENT_TO_INSURER,
                "Claim sent to insurer",
                "Your claim has been sent to the external insurance portal and is awaiting their decision."
        );

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
        // Reimbursement becomes known at approval time
        claim.setReimbursementAmount(montant);
        claim.setReason(null);
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
        // Schema expects non-null reimbursement_amount
        claim.setReimbursementAmount(0.0);
        claim.setReason(null);
        claimRepository.save(claim);
        return toResponseDTO(claim);
    }

    @Override
    public void deleteClaim(Long id) {
        // Throws if missing to make debugging easier
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));

        // JPA mappings (cascade + orphanRemoval) handle remboursements + files cleanup where configured.
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
                .reimbursementAmount(claim.getReimbursementAmount())
                .insuranceCompany(claim.getInsuranceCompany())
                .insuranceGrade(claim.getInsuranceGrade())
                .reason(claim.getReason())
                .status(claim.getStatus().name())
                .externalRef(claim.getExternalRef())
                .filePaths(claim.getFilePaths())
                .userId(claim.getUserId())
                .remboursements(rembDtos)
                .build();
    }

    private Double calculateReimbursement(Double amount, Double insuranceGrade) {
        if (amount == null || insuranceGrade == null) {
            return 0.0;
        }
        int grade = insuranceGrade.intValue();
        // Keep in sync with Angular `INSURANCE_GRADES` percentages
        int percentage;
        switch (grade) {
            case 1 -> percentage = 10;
            case 2 -> percentage = 12;
            case 3 -> percentage = 18;
            case 4 -> percentage = 25;
            case 5 -> percentage = 45;
            default -> percentage = 0;
        }
        return Math.round((amount * percentage / 100.0) * 100.0) / 100.0;
    }

    private List<String> buildAttachmentUrls(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>(filePaths.size());
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        for (String path : filePaths) {
            String encoded = URLEncoder.encode(path, StandardCharsets.UTF_8);
            urls.add(base + "/api/files/open?path=" + encoded);
        }
        return urls;
    }
}
