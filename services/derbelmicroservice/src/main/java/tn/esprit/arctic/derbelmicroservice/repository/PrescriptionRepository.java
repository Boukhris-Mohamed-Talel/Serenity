package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    /** Charge medicalRecord pour éviter LazyInitializationException (open-in-view désactivé). */
    @EntityGraph(attributePaths = {"medicalRecord"})
    @Query("SELECT p FROM Prescription p")
    Page<Prescription> findAll(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Prescription p JOIN FETCH p.medicalRecord WHERE p.id = :id")
    Optional<Prescription> findByIdWithMedicalRecord(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Prescription p JOIN FETCH p.medicalRecord mr WHERE mr.id = :recordId")
    List<Prescription> findByMedicalRecordId(@Param("recordId") Long medicalRecordId);
}
