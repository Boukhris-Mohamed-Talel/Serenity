package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT m FROM MedicalRecord m")
    Page<MedicalRecord> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT m FROM MedicalRecord m WHERE m.doctorId = :doctorId")
    Page<MedicalRecord> findAllByDoctorId(@Param("doctorId") Long doctorId, Pageable pageable);

    @Query("SELECT DISTINCT m FROM MedicalRecord m JOIN FETCH m.patient WHERE m.id = :id")
    Optional<MedicalRecord> findByIdWithPatient(@Param("id") Long id);

    @Query("SELECT DISTINCT m FROM MedicalRecord m JOIN FETCH m.patient WHERE m.id = :id AND m.doctorId = :doctorId")
    Optional<MedicalRecord> findByIdWithPatientAndDoctorId(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Query("SELECT DISTINCT m FROM MedicalRecord m JOIN FETCH m.patient p WHERE p.id = :patientId")
    List<MedicalRecord> findByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT DISTINCT m FROM MedicalRecord m JOIN FETCH m.patient p WHERE p.id = :patientId AND m.doctorId = :doctorId")
    List<MedicalRecord> findByPatientIdAndDoctorId(@Param("patientId") Long patientId, @Param("doctorId") Long doctorId);
}
