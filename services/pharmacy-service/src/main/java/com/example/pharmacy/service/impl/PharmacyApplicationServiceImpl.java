package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.*;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PharmacyApplicationStatus;
import com.example.pharmacy.entity.PharmacyOnboardingApplication;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.PharmacyOnboardingApplicationRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.PharmacyApplicationDocumentPayload;
import com.example.pharmacy.service.PharmacyApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyApplicationServiceImpl implements PharmacyApplicationService {

    private static final Set<PharmacyApplicationStatus> ACTIVE_STATUSES = EnumSet.of(
        PharmacyApplicationStatus.SUBMITTED,
        PharmacyApplicationStatus.APPROVED
    );

    private final PharmacyOnboardingApplicationRepository applicationRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PharmacyApplicationDocumentStorageService documentStorageService;
    private final UserRoleAssignmentClient userRoleAssignmentClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public PharmacyApplicationResponseDTO getMyApplication() {
        Long userId = currentUserService.getCurrentUserId();
        PharmacyOnboardingApplication application = applicationRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy application", "userId", userId));
        return toApplicantResponse(application);
    }

    @Override
    public PharmacyApplicationResponseDTO submitMyApplication(
        PharmacyApplicationSubmitRequestDTO request,
        MultipartFile cinDocument,
        MultipartFile cnoptProofDocument,
        MultipartFile legalProofDocument
    ) {
        Long userId = currentUserService.getCurrentUserId();
        PharmacyOnboardingApplication application = applicationRepository.findByUserId(userId)
            .orElseGet(() -> PharmacyOnboardingApplication.builder()
                .userId(userId)
                .status(PharmacyApplicationStatus.REJECTED)
                .build());

        if (application.getStatus() == PharmacyApplicationStatus.APPROVED) {
            throw new IllegalStateException("Application is already approved");
        }
        if (application.getStatus() == PharmacyApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Application is already submitted and pending review");
        }

        String normalizedCnop = normalizeIdentifier(request.getCnopNumber());
        String normalizedAuthorizationRef = normalizeIdentifier(request.getAuthorizationReferenceNumber());

        ensureNoDuplicateCnop(normalizedCnop, application.getId());
        ensureNoDuplicateAuthorizationReference(normalizedAuthorizationRef, application.getId());
        if (pharmacyRepository.existsByLicenseNumberIgnoreCase(normalizedAuthorizationRef)) {
            throw new IllegalStateException("Authorization reference already belongs to an approved pharmacy");
        }

        application.setFirstName(trimToNull(request.getFirstName()));
        application.setLastName(trimToNull(request.getLastName()));
        application.setEmail(trimToNull(request.getEmail()));
        application.setCinNumber(normalizeIdentifier(request.getCinNumber()));
        application.setCnopNumber(normalizedCnop);
        application.setPharmacyName(trimToNull(request.getPharmacyName()));
        application.setAuthorizationReferenceNumber(normalizedAuthorizationRef);
        application.setPhone(trimToNull(request.getPhone()));
        application.setOpeningHours(trimToNull(request.getOpeningHours()));
        application.setAddressLine(trimToNull(request.getAddressLine()));
        application.setCity(trimToNull(request.getCity()));
        application.setGovernorate(trimToNull(request.getGovernorate()));
        application.setLatitude(request.getLatitude());
        application.setLongitude(request.getLongitude());

        String cinPath = resolveAndStoreRequiredDocument(
            userId,
            "cin",
            cinDocument,
            application.getCinDocumentPath()
        );
        String cnoptPath = resolveAndStoreRequiredDocument(
            userId,
            "cnopt",
            cnoptProofDocument,
            application.getCnoptProofPath()
        );
        String legalPath = resolveAndStoreRequiredDocument(
            userId,
            "legal-proof",
            legalProofDocument,
            application.getPharmacyAuthorizationPath()
        );

        application.setCinDocumentPath(cinPath);
        application.setCnoptProofPath(cnoptPath);
        application.setPharmacyAuthorizationPath(legalPath);

        application.setStatus(PharmacyApplicationStatus.SUBMITTED);
        application.setSubmittedAt(LocalDateTime.now());
        application.setReviewedAt(null);
        application.setReviewedByAdminId(null);
        application.setReviewComment(null);

        PharmacyOnboardingApplication saved = applicationRepository.save(application);
        return toApplicantResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPharmacyApplicationSummaryDTO> listApplications(PharmacyApplicationStatus status) {
        List<PharmacyOnboardingApplication> applications = status == null
            ? applicationRepository.findAllByOrderBySubmittedAtDesc()
            : applicationRepository.findAllByStatusOrderBySubmittedAtDesc(status);

        return applications.stream()
            .map(this::toAdminSummary)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPharmacyApplicationDetailsDTO getApplicationDetails(Long applicationId) {
        PharmacyOnboardingApplication application = findById(applicationId);
        return toAdminDetails(application);
    }

    @Override
    public AdminPharmacyApplicationDetailsDTO approveApplication(Long applicationId) {
        PharmacyOnboardingApplication application = findById(applicationId);
        if (application.getStatus() == PharmacyApplicationStatus.APPROVED) {
            throw new IllegalStateException("Application is already approved");
        }
        if (application.getStatus() != PharmacyApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted applications can be approved");
        }

        validateRequiredDocumentPaths(application);
        ensureNoDuplicateCnop(application.getCnopNumber(), application.getId());
        ensureNoDuplicateAuthorizationReference(application.getAuthorizationReferenceNumber(), application.getId());
        if (pharmacyRepository.existsByLicenseNumberIgnoreCase(application.getAuthorizationReferenceNumber())) {
            throw new IllegalStateException("Authorization reference already belongs to an approved pharmacy");
        }
        if (pharmacyRepository.findByOwnerUserId(application.getUserId()).isPresent()) {
            throw new IllegalStateException("User already has a live pharmacy profile");
        }

        pharmacyRepository.save(toLivePharmacy(application));
        userRoleAssignmentClient.assignPharmacistRole(application.getUserId());

        application.setStatus(PharmacyApplicationStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedByAdminId(currentUserService.getCurrentUserId());
        application.setReviewComment(null);

        PharmacyOnboardingApplication saved = applicationRepository.save(application);
        return toAdminDetails(saved);
    }

    @Override
    public AdminPharmacyApplicationDetailsDTO rejectApplication(Long applicationId, String reviewComment) {
        PharmacyOnboardingApplication application = findById(applicationId);
        if (application.getStatus() == PharmacyApplicationStatus.APPROVED) {
            throw new IllegalStateException("Approved applications cannot be rejected");
        }

        String trimmedComment = trimToNull(reviewComment);
        if (!StringUtils.hasText(trimmedComment)) {
            throw new IllegalArgumentException("Review comment is required for rejection");
        }

        application.setStatus(PharmacyApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedByAdminId(currentUserService.getCurrentUserId());
        application.setReviewComment(trimmedComment);

        PharmacyOnboardingApplication saved = applicationRepository.save(application);
        return toAdminDetails(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyApplicationDocumentPayload getApplicationDocument(Long applicationId, String documentType) {
        PharmacyOnboardingApplication application = findById(applicationId);
        String path = switch (normalizeDocumentType(documentType)) {
            case "cin" -> application.getCinDocumentPath();
            case "cnopt" -> application.getCnoptProofPath();
            case "legal-proof" -> application.getPharmacyAuthorizationPath();
            default -> throw new IllegalArgumentException("Unsupported document type");
        };

        if (!StringUtils.hasText(path)) {
            throw new ResourceNotFoundException("Application document", "type", documentType);
        }

        Resource resource = documentStorageService.loadDocument(path);
        String contentType = documentStorageService.resolveContentType(path);
        String fileName = extractFileName(path);

        return new PharmacyApplicationDocumentPayload(resource, contentType, fileName);
    }

    private Pharmacy toLivePharmacy(PharmacyOnboardingApplication application) {
        return Pharmacy.builder()
            .ownerUserId(application.getUserId())
            .name(application.getPharmacyName())
            .licenseNumber(application.getAuthorizationReferenceNumber())
            .phone(application.getPhone())
            .openingHours(application.getOpeningHours())
            .addressLine(application.getAddressLine())
            .city(application.getCity())
            .governorate(application.getGovernorate())
            .latitude(application.getLatitude())
            .longitude(application.getLongitude())
            .supportsEmergency(false)
            .build();
    }

    private String resolveAndStoreRequiredDocument(
        Long userId,
        String documentType,
        MultipartFile uploadedFile,
        String currentPath
    ) {
        if (uploadedFile != null && !uploadedFile.isEmpty()) {
            return documentStorageService.storeDocument(userId, documentType, uploadedFile, currentPath);
        }
        if (StringUtils.hasText(currentPath)) {
            return currentPath;
        }
        throw new IllegalArgumentException("Missing required " + documentType + " document");
    }

    private void validateRequiredDocumentPaths(PharmacyOnboardingApplication application) {
        if (!StringUtils.hasText(application.getCinDocumentPath())
            || !StringUtils.hasText(application.getCnoptProofPath())
            || !StringUtils.hasText(application.getPharmacyAuthorizationPath())) {
            throw new IllegalStateException("Application is missing one or more required documents");
        }
    }

    private PharmacyOnboardingApplication findById(Long applicationId) {
        return applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy application", "id", applicationId));
    }

    private void ensureNoDuplicateCnop(String cnopNumber, Long currentApplicationId) {
        Optional<PharmacyOnboardingApplication> duplicate = applicationRepository
            .findFirstByCnopNumberIgnoreCaseAndStatusIn(cnopNumber, ACTIVE_STATUSES);

        if (duplicate.isPresent() && !duplicate.get().getId().equals(currentApplicationId)) {
            throw new IllegalStateException("CNOP/CNOPT number is already used by another active application");
        }
    }

    private void ensureNoDuplicateAuthorizationReference(String authorizationReference, Long currentApplicationId) {
        Optional<PharmacyOnboardingApplication> duplicate = applicationRepository
            .findFirstByAuthorizationReferenceNumberIgnoreCaseAndStatusIn(authorizationReference, ACTIVE_STATUSES);

        if (duplicate.isPresent() && !duplicate.get().getId().equals(currentApplicationId)) {
            throw new IllegalStateException("Authorization reference is already used by another active application");
        }
    }

    private String normalizeIdentifier(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDocumentType(String documentType) {
        return documentType == null ? "" : documentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extractFileName(String path) {
        int slashIndex = path.lastIndexOf('/');
        return slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
    }

    private PharmacyApplicationResponseDTO toApplicantResponse(PharmacyOnboardingApplication application) {
        return PharmacyApplicationResponseDTO.builder()
            .id(application.getId())
            .userId(application.getUserId())
            .status(application.getStatus())
            .submittedAt(toIso(application.getSubmittedAt()))
            .reviewedAt(toIso(application.getReviewedAt()))
            .reviewedByAdminId(application.getReviewedByAdminId())
            .reviewComment(application.getReviewComment())
            .firstName(application.getFirstName())
            .lastName(application.getLastName())
            .email(application.getEmail())
            .cinNumber(application.getCinNumber())
            .cnopNumber(application.getCnopNumber())
            .pharmacyName(application.getPharmacyName())
            .authorizationReferenceNumber(application.getAuthorizationReferenceNumber())
            .phone(application.getPhone())
            .openingHours(application.getOpeningHours())
            .addressLine(application.getAddressLine())
            .city(application.getCity())
            .governorate(application.getGovernorate())
            .latitude(application.getLatitude())
            .longitude(application.getLongitude())
            .cinDocumentUploaded(StringUtils.hasText(application.getCinDocumentPath()))
            .cnoptProofUploaded(StringUtils.hasText(application.getCnoptProofPath()))
            .legalDocumentUploaded(StringUtils.hasText(application.getPharmacyAuthorizationPath()))
            .build();
    }

    private AdminPharmacyApplicationSummaryDTO toAdminSummary(PharmacyOnboardingApplication application) {
        return AdminPharmacyApplicationSummaryDTO.builder()
            .id(application.getId())
            .userId(application.getUserId())
            .applicantName((application.getFirstName() + " " + application.getLastName()).trim())
            .email(application.getEmail())
            .pharmacyName(application.getPharmacyName())
            .city(application.getCity())
            .governorate(application.getGovernorate())
            .status(application.getStatus())
            .submittedAt(toIso(application.getSubmittedAt()))
            .reviewedAt(toIso(application.getReviewedAt()))
            .build();
    }

    private AdminPharmacyApplicationDetailsDTO toAdminDetails(PharmacyOnboardingApplication application) {
        Long id = application.getId();
        return AdminPharmacyApplicationDetailsDTO.builder()
            .id(id)
            .userId(application.getUserId())
            .status(application.getStatus())
            .submittedAt(toIso(application.getSubmittedAt()))
            .reviewedAt(toIso(application.getReviewedAt()))
            .reviewedByAdminId(application.getReviewedByAdminId())
            .reviewComment(application.getReviewComment())
            .firstName(application.getFirstName())
            .lastName(application.getLastName())
            .email(application.getEmail())
            .cinNumber(application.getCinNumber())
            .cnopNumber(application.getCnopNumber())
            .pharmacyName(application.getPharmacyName())
            .authorizationReferenceNumber(application.getAuthorizationReferenceNumber())
            .phone(application.getPhone())
            .openingHours(application.getOpeningHours())
            .addressLine(application.getAddressLine())
            .city(application.getCity())
            .governorate(application.getGovernorate())
            .latitude(application.getLatitude())
            .longitude(application.getLongitude())
            .cinDocumentUrl(StringUtils.hasText(application.getCinDocumentPath())
                ? "/api/pharmacy/admin/applications/" + id + "/documents/cin"
                : null)
            .cnoptProofUrl(StringUtils.hasText(application.getCnoptProofPath())
                ? "/api/pharmacy/admin/applications/" + id + "/documents/cnopt"
                : null)
            .legalDocumentUrl(StringUtils.hasText(application.getPharmacyAuthorizationPath())
                ? "/api/pharmacy/admin/applications/" + id + "/documents/legal-proof"
                : null)
            .build();
    }

    private String toIso(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
