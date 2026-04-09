package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.PrescriptionItem;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);

    @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.medicine.id = :medicineId")
    List<PrescriptionItem> findByMedicineId(@Param("medicineId") Long medicineId);
}
