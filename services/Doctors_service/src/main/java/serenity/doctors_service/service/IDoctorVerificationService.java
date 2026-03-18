package serenity.doctors_service.service;

import serenity.doctors_service.entity.DoctorVerification;

import java.util.List;
import java.util.Optional;

public interface IDoctorVerificationService {
    DoctorVerification save(DoctorVerification verification);

    List<DoctorVerification> findAll();

    Optional<DoctorVerification> findById(Long verification_id);

    void deleteById(Long verification_id);
}
