package serenity.doctors_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import serenity.doctors_service.entity.DoctorVerification;

import java.util.List;

public interface DoctorVerificationRepository  extends JpaRepository<DoctorVerification, Long> {
    List<DoctorVerification> findByDoctorId(Long id);
}
