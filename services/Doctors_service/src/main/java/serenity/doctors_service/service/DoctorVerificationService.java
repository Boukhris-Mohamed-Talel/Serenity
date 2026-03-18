package serenity.doctors_service.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.repository.DoctorVerificationRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorVerificationService implements IDoctorVerificationService {

    @Autowired
    private DoctorVerificationRepository repository;


    @Override
    public DoctorVerification save(DoctorVerification verification) {
        return repository.save(verification);
    }


    @Override
    public List<DoctorVerification> findAll() {
        return repository.findAll();
    }


    @Override
    public Optional<DoctorVerification> findById(Long verification_id) {
        return repository.findById(verification_id);
    }


    @Override
    public void deleteById(Long verification_id) {
        repository.deleteById(verification_id);
    }


}
