package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.Patient;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Page<Patient> findByDoctorId(Long doctorId, Pageable pageable);

    Optional<Patient> findByIdAndDoctorId(Long id, Long doctorId);

    @Query("SELECT p FROM Patient p WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%',:name,'%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%',:name,'%'))")
    List<Patient> searchByName(@Param("name") String name);

    @Query("SELECT p FROM Patient p WHERE p.doctorId = :doctorId AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%',:name,'%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%',:name,'%')))")
    List<Patient> searchByNameAndDoctorId(@Param("name") String name, @Param("doctorId") Long doctorId);

    long countByDoctorId(Long doctorId);
}
