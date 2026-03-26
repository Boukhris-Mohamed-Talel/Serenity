package com.example.pharmacy.repository;

import com.example.pharmacy.entity.PrescriptionOrder;
import com.example.pharmacy.entity.PrescriptionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrescriptionOrderRepository extends JpaRepository<PrescriptionOrder, Long> {
    List<PrescriptionOrder> findByPharmacyOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
    List<PrescriptionOrder> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<PrescriptionOrder> findByPatientIdAndPharmacyIsNullAndStatusOrderByCreatedAtDesc(Long patientId, PrescriptionStatus status);

    @Query("""
        select p from PrescriptionOrder p
        where lower(p.patientName) like lower(concat('%', :query, '%'))
        order by p.createdAt desc
        """)
    List<PrescriptionOrder> findRecentByPatientNameContaining(@Param("query") String query, Pageable pageable);
}
