package com.example.pharmacy.repository;

import com.example.pharmacy.entity.PharmacyPrescription;
import com.example.pharmacy.entity.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PharmacyPrescriptionRepository extends JpaRepository<PharmacyPrescription, Long> {

    List<PharmacyPrescription> findByAssignedPharmacyOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<PharmacyPrescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<PharmacyPrescription> findByPatientIdAndAssignedPharmacyIsNullAndStatusOrderByCreatedAtDesc(
        Long patientId,
        PrescriptionStatus status
    );

    List<PharmacyPrescription> findByAssignedPharmacyId(Long pharmacyId);
}
