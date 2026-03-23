package com.example.pharmacy.repository;

import com.example.pharmacy.entity.PrescriptionOrder;
import com.example.pharmacy.entity.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionOrderRepository extends JpaRepository<PrescriptionOrder, Long> {
    List<PrescriptionOrder> findByPharmacyOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
    List<PrescriptionOrder> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<PrescriptionOrder> findByPatientIdAndPharmacyIsNullAndStatusOrderByCreatedAtDesc(Long patientId, PrescriptionStatus status);
}
