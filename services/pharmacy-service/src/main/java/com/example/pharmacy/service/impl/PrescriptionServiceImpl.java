package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PrescriptionCreateRequestDTO;
import com.example.pharmacy.dto.PrescriptionLineCreateRequestDTO;
import com.example.pharmacy.dto.PrescriptionLineResponseDTO;
import com.example.pharmacy.dto.PrescriptionResponseDTO;
import com.example.pharmacy.dto.PrescriptionStatusUpdateRequestDTO;
import com.example.pharmacy.entity.PatientPharmacyPreference;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PrescriptionLine;
import com.example.pharmacy.entity.PrescriptionOrder;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.PatientPharmacyPreferenceRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.repository.PrescriptionOrderRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionOrderRepository prescriptionOrderRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PatientPharmacyPreferenceRepository preferenceRepository;
    private final CurrentUserService currentUserService;

    @Override
    public PrescriptionResponseDTO createPrescription(PrescriptionCreateRequestDTO request) {
        Long doctorId = currentUserService.getCurrentUserId();
        Pharmacy pharmacy = resolvePharmacyForPrescription(request);
        List<PrescriptionLineCreateRequestDTO> lineRequests = resolveLineRequests(request);
        PrescriptionLineCreateRequestDTO summaryLine = lineRequests.get(0);

        PrescriptionOrder order = PrescriptionOrder.builder()
            .pharmacy(pharmacy)
            .doctorId(doctorId)
            .patientId(request.getPatientId())
            .doctorName(request.getDoctorName())
            .patientName(request.getPatientName())
            .medicationName(summaryLine.getMedicationName())
            .dosage(summaryLine.getDosage())
            .quantity(summaryLine.getQuantity())
            .instructions(summaryLine.getInstructions())
            .status(PrescriptionStatus.PENDING)
            .build();

        List<PrescriptionLine> lines = new ArrayList<>();
        for (PrescriptionLineCreateRequestDTO lineRequest : lineRequests) {
            lines.add(PrescriptionLine.builder()
                .prescriptionOrder(order)
                .medicationName(lineRequest.getMedicationName())
                .dosage(lineRequest.getDosage())
                .quantity(lineRequest.getQuantity())
                .instructions(lineRequest.getInstructions())
                .build());
        }
        order.setMedicineLines(lines);

        return toResponse(prescriptionOrderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getMyInbox() {
        Long pharmacistId = currentUserService.getCurrentUserId();
        return prescriptionOrderRepository.findByPharmacyOwnerUserIdOrderByCreatedAtDesc(pharmacistId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getMyPatientPrescriptions() {
        Long patientId = currentUserService.getCurrentUserId();
        return prescriptionOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescription(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        PrescriptionOrder order = prescriptionOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        boolean isOwnerPharmacist = order.getPharmacy() != null
            && order.getPharmacy().getOwnerUserId().equals(currentUserId);
        boolean isPatient = order.getPatientId().equals(currentUserId);
        boolean isDoctor = order.getDoctorId().equals(currentUserId);

        if (!(isOwnerPharmacist || isPatient || isDoctor)) {
            throw new IllegalStateException("You cannot access this prescription");
        }

        return toResponse(order);
    }

    @Override
    public PrescriptionResponseDTO updatePrescriptionStatus(Long id, PrescriptionStatusUpdateRequestDTO request) {
        Long pharmacistId = currentUserService.getCurrentUserId();
        PrescriptionOrder order = prescriptionOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        if (order.getPharmacy() == null) {
            throw new IllegalStateException("This prescription is not yet assigned to a pharmacy");
        }

        if (!order.getPharmacy().getOwnerUserId().equals(pharmacistId)) {
            throw new IllegalStateException("You can only update prescriptions assigned to your pharmacy");
        }

        PrescriptionStatus nextStatus = request.getStatus();
        if (nextStatus == PrescriptionStatus.PENDING || nextStatus == PrescriptionStatus.EXPIRED) {
            throw new IllegalArgumentException("Invalid status update for pharmacist workflow");
        }

        if (nextStatus == PrescriptionStatus.REJECTED
            && (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new IllegalArgumentException("Rejection reason is required when rejecting a prescription");
        }

        order.setStatus(nextStatus);
        order.setRejectionReason(request.getRejectionReason());

        if (nextStatus == PrescriptionStatus.READY_FOR_PICKUP) {
            order.setReadyAt(LocalDateTime.now());
        }

        return toResponse(prescriptionOrderRepository.save(order));
    }

    private PrescriptionResponseDTO toResponse(PrescriptionOrder order) {
        List<PrescriptionLineResponseDTO> lines = order.getMedicineLines() == null
            ? List.of()
            : order.getMedicineLines().stream().map(this::toLineResponse).toList();

        PrescriptionLineResponseDTO summaryLine = lines.isEmpty() ? null : lines.get(0);

        return PrescriptionResponseDTO.builder()
            .id(order.getId())
            .pharmacyId(order.getPharmacy() != null ? order.getPharmacy().getId() : null)
            .pharmacyName(order.getPharmacy() != null ? order.getPharmacy().getName() : null)
            .doctorId(order.getDoctorId())
            .patientId(order.getPatientId())
            .doctorName(order.getDoctorName())
            .patientName(order.getPatientName())
            .assignedToPharmacy(order.getPharmacy() != null)
            .assignmentMessage(order.getPharmacy() == null ? "Patient has no default pharmacy yet" : null)
            .medicationName(summaryLine != null ? summaryLine.getMedicationName() : order.getMedicationName())
            .dosage(summaryLine != null ? summaryLine.getDosage() : order.getDosage())
            .quantity(summaryLine != null ? summaryLine.getQuantity() : order.getQuantity())
            .instructions(summaryLine != null ? summaryLine.getInstructions() : order.getInstructions())
            .medicineLines(lines)
            .status(order.getStatus())
            .rejectionReason(order.getRejectionReason())
            .readyAt(order.getReadyAt() != null ? order.getReadyAt().toString() : null)
            .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
            .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)
            .build();
    }

    private PrescriptionLineResponseDTO toLineResponse(PrescriptionLine line) {
        return PrescriptionLineResponseDTO.builder()
            .id(line.getId())
            .medicationName(line.getMedicationName())
            .dosage(line.getDosage())
            .quantity(line.getQuantity())
            .instructions(line.getInstructions())
            .build();
    }

    private Pharmacy resolvePharmacyForPrescription(PrescriptionCreateRequestDTO request) {
        if (request.getPharmacyId() != null) {
            return pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", request.getPharmacyId()));
        }

        PatientPharmacyPreference preference = preferenceRepository.findByPatientId(request.getPatientId())
            .orElse(null);

        return preference != null ? preference.getDefaultPharmacy() : null;
    }

    private List<PrescriptionLineCreateRequestDTO> resolveLineRequests(PrescriptionCreateRequestDTO request) {
        List<PrescriptionLineCreateRequestDTO> explicitLines = request.getMedicineLines();
        if (explicitLines == null || explicitLines.isEmpty()) {
            throw new IllegalArgumentException("At least one medicine line is required");
        }

        return explicitLines.stream().map(this::validateAndNormalizeLine).toList();
    }

    private PrescriptionLineCreateRequestDTO validateAndNormalizeLine(PrescriptionLineCreateRequestDTO line) {
        if (line == null) {
            throw new IllegalArgumentException("At least one medicine line is required");
        }

        String medicationName = line.getMedicationName() == null ? null : line.getMedicationName().trim();
        String dosage = line.getDosage() == null ? null : line.getDosage().trim();

        if (medicationName == null || medicationName.isEmpty()) {
            throw new IllegalArgumentException("Medication name is required for every medicine line");
        }

        if (dosage == null || dosage.isEmpty()) {
            throw new IllegalArgumentException("Dosage is required for every medicine line");
        }

        if (line.getQuantity() == null || line.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero for every medicine line");
        }

        return PrescriptionLineCreateRequestDTO.builder()
            .medicationName(medicationName)
            .dosage(dosage)
            .quantity(line.getQuantity())
            .instructions(line.getInstructions())
            .build();
    }
}
