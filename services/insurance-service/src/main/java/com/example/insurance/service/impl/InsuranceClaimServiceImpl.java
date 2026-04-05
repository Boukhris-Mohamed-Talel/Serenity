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
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceClaimServiceImpl implements InsuranceClaimService {
    private static final Set<String> ALLOWED_INSURANCE_COMPANIES = Set.of(
            "Insurance 1", "Insurance 2", "Insurance 3", "Insurance 4", "Insurance 5"
    );
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png"
    );
    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

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
        validateBusinessRules(request, files);

        List<String> filePaths = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            filePaths = saveFiles(files, userId);
        }

        String externalRef = UUID.randomUUID().toString();

        // Used to populate the DB column `reimbursement_amount` (non-null in schema)
        Double reimbursementAmount = calculateReimbursement(request.getAmount(), request.getInsuranceGrade());

        InsuranceClaim claim = InsuranceClaim.builder()
                .description(request.getDescription().trim())
                .amount(request.getAmount())
                .reimbursementAmount(reimbursementAmount)
                .insuranceCompany(request.getInsuranceCompany().trim())
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
        return getClaimsByUserId(userId, null, null, null, null, "claimDate", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getClaimsByUserId(
            Long userId,
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        return findClaims(userId, status, insuranceCompany, fromDate, toDate, sortBy, sortDir)
                .stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getAllClaims() {
        return getAllClaims(null, null, null, null, "claimDate", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceClaimResponseDTO> getAllClaims(
            String status,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        return findClaims(null, status, insuranceCompany, fromDate, toDate, sortBy, sortDir)
                .stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceClaimResponseDTO getClaimById(Long id, Long requesterUserId, boolean isAdmin) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id));
        if (!isAdmin && !claim.getUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this claim");
        }
        return toResponseDTO(claim);
    }

    @Override
    public InsuranceClaimResponseDTO approveClaim(Long id, Double montant) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id));
        claim.setStatus(ClaimStatus.REJECTED);
        // Schema expects non-null reimbursement_amount
        claim.setReimbursementAmount(0.0);
        claim.setReason(null);
        claimRepository.save(claim);
        return toResponseDTO(claim);
    }

    @Override
    public void deleteClaim(Long id) {
        InsuranceClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id));

        // JPA mappings (cascade + orphanRemoval) handle remboursements + files cleanup where configured.
        claimRepository.delete(claim);
    }

    private List<String> saveFiles(List<MultipartFile> files, Long userId) {
        List<String> paths = new ArrayList<>();
        try {
            Path uploadPath = Paths.get(uploadDir, "claims", userId.toString());
            Files.createDirectories(uploadPath);
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
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

    private void validateBusinessRules(InsuranceClaimRequestDTO request, List<MultipartFile> files) {
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
        }
        if (request.getInsuranceCompany() == null || !ALLOWED_INSURANCE_COMPANIES.contains(request.getInsuranceCompany().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insurance company is invalid");
        }
        if (request.getInsuranceGrade() == null || request.getInsuranceGrade() < 1.0 || request.getInsuranceGrade() > 5.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insurance grade must be between 1 and 5");
        }

        if (files == null || files.isEmpty()) {
            return;
        }
        if (files.size() > MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 5 files are allowed");
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each file must be <= 10 MB");
            }
            String type = file.getContentType();
            if (type == null || !ALLOWED_FILE_TYPES.contains(type.toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPG and PNG files are allowed");
            }
        }
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

    private List<InsuranceClaim> findClaims(
            Long userId,
            String statusRaw,
            String insuranceCompany,
            LocalDate fromDate,
            LocalDate toDate,
            String sortByRaw,
            String sortDirRaw
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate cannot be after toDate");
        }

        ClaimStatus status = parseStatus(statusRaw);
        Sort sort = buildSort(sortByRaw, sortDirRaw);

        Specification<InsuranceClaim> spec = Specification.where(null);

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (insuranceCompany != null && !insuranceCompany.isBlank()) {
            String lowered = insuranceCompany.trim().toLowerCase();
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("insuranceCompany")), "%" + lowered + "%"));
        }
        if (fromDate != null) {
            var fromInstant = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("claimDate"), java.util.Date.from(fromInstant)));
        }
        if (toDate != null) {
            var toInstant = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("claimDate"), java.util.Date.from(toInstant)));
        }

        return claimRepository.findAll(spec, sort);
    }

    private ClaimStatus parseStatus(String statusRaw) {
        if (statusRaw == null || statusRaw.isBlank()) {
            return null;
        }
        try {
            return ClaimStatus.valueOf(statusRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + statusRaw);
        }
    }

    private Sort buildSort(String sortByRaw, String sortDirRaw) {
        String sortBy = sortByRaw == null ? "claimDate" : sortByRaw.trim();
        String field = switch (sortBy.toLowerCase()) {
            case "date", "claimdate" -> "claimDate";
            case "amount" -> "amount";
            case "reimbursement", "reimbursementamount" -> "reimbursementAmount";
            case "status" -> "status";
            case "insurancecompany", "company" -> "insuranceCompany";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortBy: " + sortByRaw);
        };

        String sortDir = sortDirRaw == null ? "desc" : sortDirRaw.trim().toLowerCase();
        Sort.Direction direction = switch (sortDir) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortDir: " + sortDirRaw);
        };
        return Sort.by(direction, field);
    }
}
