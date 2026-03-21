package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PrescriptionCreateRequestDTO;
import com.example.pharmacy.dto.PrescriptionResponseDTO;
import com.example.pharmacy.dto.PrescriptionStatusUpdateRequestDTO;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PrescriptionOrder;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.repository.PrescriptionOrderRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionOrderRepository prescriptionOrderRepository;
    private final PharmacyRepository pharmacyRepository;
    private final CurrentUserService currentUserService;

    @Override
    public PrescriptionResponseDTO createPrescription(PrescriptionCreateRequestDTO request) {
        Long doctorId = currentUserService.getCurrentUserId();
        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", request.getPharmacyId()));

        PrescriptionOrder order = PrescriptionOrder.builder()
            .pharmacy(pharmacy)
            .doctorId(doctorId)
            .patientId(request.getPatientId())
            .doctorName(request.getDoctorName())
            .patientName(request.getPatientName())
            .medicationName(request.getMedicationName())
            .dosage(request.getDosage())
            .quantity(request.getQuantity())
            .instructions(request.getInstructions())
            .status(PrescriptionStatus.PENDING)
            .build();

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

        boolean isOwnerPharmacist = order.getPharmacy().getOwnerUserId().equals(currentUserId);
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
        return PrescriptionResponseDTO.builder()
            .id(order.getId())
            .pharmacyId(order.getPharmacy().getId())
            .pharmacyName(order.getPharmacy().getName())
            .doctorId(order.getDoctorId())
            .patientId(order.getPatientId())
            .doctorName(order.getDoctorName())
            .patientName(order.getPatientName())
            .medicationName(order.getMedicationName())
            .dosage(order.getDosage())
            .quantity(order.getQuantity())
            .instructions(order.getInstructions())
            .status(order.getStatus())
            .rejectionReason(order.getRejectionReason())
            .readyAt(order.getReadyAt() != null ? order.getReadyAt().toString() : null)
            .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
            .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)
            .build();
    }
}
