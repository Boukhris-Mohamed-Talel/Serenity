package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
}
