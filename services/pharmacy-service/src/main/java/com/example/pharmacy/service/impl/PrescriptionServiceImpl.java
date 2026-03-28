package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.*;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PharmacyPrescription;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.PharmacyPrescriptionRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final Map<PrescriptionStatus, List<PrescriptionStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        PrescriptionStatus.PENDING, List.of(PrescriptionStatus.ACCEPTED, PrescriptionStatus.REJECTED),
        PrescriptionStatus.ACCEPTED, List.of(PrescriptionStatus.READY_FOR_PICKUP, PrescriptionStatus.REJECTED),
        PrescriptionStatus.READY_FOR_PICKUP, List.of(PrescriptionStatus.COLLECTED),
        PrescriptionStatus.REJECTED, List.of(),
        PrescriptionStatus.COLLECTED, List.of(),
        PrescriptionStatus.EXPIRED, List.of()
    );

    private final PharmacyPrescriptionRepository pharmacyPrescriptionRepository;
    private final PharmacyRepository pharmacyRepository;
    private final CurrentUserService currentUserService;
    private final PrescriptionLineQueryService prescriptionLineQueryService;
    private final PrescriptionAlternativeService prescriptionAlternativeService;
    private final PrescriptionResponseMapper prescriptionResponseMapper;

    @Override
    public List<PrescriptionResponseDTO> getMyInbox() {
        Long pharmacistId = currentUserService.getCurrentUserId();
        List<PharmacyPrescription> workflows = pharmacyPrescriptionRepository
            .findByAssignedPharmacyOwnerUserIdOrderByCreatedAtDesc(pharmacistId);

        return toResponses(workflows);
    }

    @Override
    public List<PrescriptionResponseDTO> getMyPatientPrescriptions() {
        Long patientId = currentUserService.getCurrentUserId();
        List<PharmacyPrescription> workflows = pharmacyPrescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return toResponses(workflows);
    }

    @Override
    public PrescriptionResponseDTO getPrescription(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = pharmacyPrescriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        boolean isOwnerPharmacist = isPharmacyOwner(workflow, currentUserId);
        boolean isPatient = workflow.getPatientId().equals(currentUserId);
        if (!(isOwnerPharmacist || isPatient)) {
            throw new AccessDeniedException("You cannot access this prescription");
        }

        List<PrescriptionLineResponseDTO> lines =
            prescriptionLineQueryService.loadLinesForSource(workflow.getSourcePrescriptionId());
        return prescriptionResponseMapper.toResponse(workflow, lines);
    }

    @Override
    public PrescriptionAlternativeResponseDTO getPatientAlternatives(Long id, Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        Long patientId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = getOwnedPatientPrescription(id, patientId);

        if (workflow.getStatus() != PrescriptionStatus.PENDING) {
            throw new IllegalStateException("Alternative pharmacy recommendations are only available for pending prescriptions");
        }

        List<PrescriptionLineResponseDTO> liveLines =
            prescriptionLineQueryService.loadLinesForSource(workflow.getSourcePrescriptionId());
        List<PrescriptionLineQueryService.RequiredLine> requiredLines =
            prescriptionLineQueryService.resolveRequiredLines(liveLines);
        if (requiredLines.isEmpty()) {
            throw new IllegalStateException("Prescription does not contain medicine lines");
        }

        return prescriptionAlternativeService.buildAlternatives(
            workflow.getId(),
            workflow.getStatus(),
            requiredLines,
            latitude,
            longitude
        );
    }

    @Override
    public PrescriptionResponseDTO reassignPatientPrescriptionPharmacy(Long id, PrescriptionPharmacyReassignRequestDTO request) {
        validateCoordinates(request.getLatitude(), request.getLongitude());
        Long patientId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = getOwnedPatientPrescription(id, patientId);

        if (workflow.getStatus() != PrescriptionStatus.PENDING) {
            throw new IllegalStateException("Only pending prescriptions can be reassigned to another pharmacy");
        }

        PrescriptionAlternativeResponseDTO alternatives = getPatientAlternatives(id, request.getLatitude(), request.getLongitude());
        Set<Long> candidatePharmacyIds = prescriptionAlternativeService.extractSelectablePharmacyIds(alternatives);

        if (!candidatePharmacyIds.contains(request.getPharmacyId())) {
            throw new IllegalArgumentException("Selected pharmacy is not part of the recommendation results");
        }

        Pharmacy selectedPharmacy = pharmacyRepository.findById(request.getPharmacyId())
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", request.getPharmacyId()));

        workflow.setAssignedPharmacy(selectedPharmacy);
        PharmacyPrescription saved = pharmacyPrescriptionRepository.save(workflow);
        List<PrescriptionLineResponseDTO> lines =
            prescriptionLineQueryService.loadLinesForSource(saved.getSourcePrescriptionId());
        return prescriptionResponseMapper.toResponse(saved, lines);
    }

    @Override
    public PrescriptionResponseDTO updatePrescriptionStatus(Long id, PrescriptionStatusUpdateRequestDTO request) {
        Long pharmacistId = currentUserService.getCurrentUserId();
        PharmacyPrescription workflow = pharmacyPrescriptionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        ensureAssignedToPharmacyOwner(workflow, pharmacistId);

        PrescriptionStatus nextStatus = request.getStatus();
        if (nextStatus == PrescriptionStatus.PENDING || nextStatus == PrescriptionStatus.EXPIRED) {
            throw new IllegalArgumentException("Invalid status update for pharmacist workflow");
        }

        if (nextStatus == workflow.getStatus()) {
            throw new IllegalArgumentException("Prescription is already in status " + workflow.getStatus());
        }

        if (!isAllowedTransition(workflow.getStatus(), nextStatus)) {
            throw new IllegalStateException(
                "Invalid status transition from " + workflow.getStatus() + " to " + nextStatus
            );
        }

        String normalizedRejectionReason = normalizeRejectionReason(request.getRejectionReason());
        if (nextStatus == PrescriptionStatus.REJECTED && normalizedRejectionReason == null) {
            throw new IllegalArgumentException("Rejection reason is required when rejecting a prescription");
        }

        workflow.setStatus(nextStatus);
        workflow.setRejectionReason(nextStatus == PrescriptionStatus.REJECTED ? normalizedRejectionReason : null);

        if (nextStatus == PrescriptionStatus.READY_FOR_PICKUP) {
            workflow.setReadyAt(LocalDateTime.now());
        } else if (nextStatus == PrescriptionStatus.ACCEPTED || nextStatus == PrescriptionStatus.REJECTED) {
            workflow.setReadyAt(null);
        }

        PharmacyPrescription saved = pharmacyPrescriptionRepository.save(workflow);
        List<PrescriptionLineResponseDTO> lines =
            prescriptionLineQueryService.loadLinesForSource(saved.getSourcePrescriptionId());
        return prescriptionResponseMapper.toResponse(saved, lines);
    }

    private List<PrescriptionResponseDTO> toResponses(List<PharmacyPrescription> workflows) {
        if (workflows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<PrescriptionLineResponseDTO>> linesBySourceId = prescriptionLineQueryService.loadLinesBySourceIds(
            workflows.stream().map(PharmacyPrescription::getSourcePrescriptionId).toList()
        );

        return workflows.stream()
            .map(workflow -> prescriptionResponseMapper.toResponse(
                workflow,
                linesBySourceId.getOrDefault(workflow.getSourcePrescriptionId(), List.of())
            ))
            .toList();
    }

    private boolean isAllowedTransition(PrescriptionStatus currentStatus, PrescriptionStatus nextStatus) {
        return ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, List.of()).contains(nextStatus);
    }

    private String normalizeRejectionReason(String rejectionReason) {
        if (rejectionReason == null) {
            return null;
        }
        String normalized = rejectionReason.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void ensureAssignedToPharmacyOwner(PharmacyPrescription workflow, Long pharmacistId) {
        if (workflow.getAssignedPharmacy() == null) {
            throw new IllegalStateException("This prescription is not yet assigned to a pharmacy");
        }

        if (!isPharmacyOwner(workflow, pharmacistId)) {
            throw new AccessDeniedException("You can only update prescriptions assigned to your pharmacy");
        }
    }

    private boolean isPharmacyOwner(PharmacyPrescription workflow, Long userId) {
        return workflow.getAssignedPharmacy() != null && workflow.getAssignedPharmacy().getOwnerUserId().equals(userId);
    }

    private PharmacyPrescription getOwnedPatientPrescription(Long prescriptionId, Long patientId) {
        PharmacyPrescription workflow = pharmacyPrescriptionRepository.findById(prescriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", prescriptionId));

        if (!workflow.getPatientId().equals(patientId)) {
            throw new AccessDeniedException("You can only manage your own prescriptions");
        }

        return workflow;
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }
}
