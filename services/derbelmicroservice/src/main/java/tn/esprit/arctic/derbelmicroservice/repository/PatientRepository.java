package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.Patient;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Page<Patient> findByDoctorId(Long doctorId, Pageable pageable);

    Optional<Patient> findByIdAndDoctorId(Long id, Long doctorId);
}
