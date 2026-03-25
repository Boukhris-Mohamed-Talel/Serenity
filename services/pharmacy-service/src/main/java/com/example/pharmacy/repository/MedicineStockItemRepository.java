package com.example.pharmacy.repository;

import com.example.pharmacy.entity.MedicineStockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MedicineStockItemRepository extends JpaRepository<MedicineStockItem, Long> {

    List<MedicineStockItem> findByPharmacyOwnerUserIdAndArchivedFalseOrderByUpdatedAtDesc(Long ownerUserId);

    List<MedicineStockItem> findByPharmacyOwnerUserIdAndArchivedTrueOrderByUpdatedAtDesc(Long ownerUserId);

    List<MedicineStockItem> findByPharmacyOwnerUserIdAndArchivedFalseAndMedicineNameContainingIgnoreCaseOrderByUpdatedAtDesc(
        Long ownerUserId,
        String medicineName
    );

    List<MedicineStockItem> findByPharmacyOwnerUserIdAndArchivedTrueAndMedicineNameContainingIgnoreCaseOrderByUpdatedAtDesc(
        Long ownerUserId,
        String medicineName
    );

    @Query("""
        select m from MedicineStockItem m
        where m.archived = false
          and m.pharmacy.id = :pharmacyId
          and lower(m.medicineName) like lower(concat('%', :query, '%'))
        order by m.medicineName asc, m.updatedAt desc
        """)
    List<MedicineStockItem> findForDoctorSuggestionInPharmacy(
        @Param("pharmacyId") Long pharmacyId,
        @Param("query") String query
    );

    @Query("""
        select m from MedicineStockItem m
        where m.archived = false
          and m.pharmacy.id in :pharmacyIds
          and lower(m.medicineName) in :medicineNames
        """)
    List<MedicineStockItem> findActiveByPharmacyIdsAndMedicineNames(
        @Param("pharmacyIds") Set<Long> pharmacyIds,
        @Param("medicineNames") Set<String> medicineNames
    );

    Optional<MedicineStockItem> findByIdAndPharmacyOwnerUserId(Long id, Long ownerUserId);
}
