package serenity.doctors_service.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.repository.DoctorVerificationRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorVerificationService implements IDoctorVerificationService {

    @Autowired
    private DoctorVerificationRepository repository;
    private final String uploadDir = "uploads/";

    @Override
    public DoctorVerification save(DoctorVerification verification) {
        return repository.save(verification);
    }

    @Override
    public DoctorVerification saveVerification(Long doctorId, MultipartFile cv, MultipartFile diploma,
                                               String licenseNumber, String nationalId) throws IOException {

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        String cvFileName = System.currentTimeMillis() + "_" + cv.getOriginalFilename();
        String diplomaFileName = System.currentTimeMillis() + "_" + diploma.getOriginalFilename();

        Path cvPath = uploadPath.resolve(cvFileName);
        Path diplomaPath = uploadPath.resolve(diplomaFileName);

        Files.copy(cv.getInputStream(), cvPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(diploma.getInputStream(), diplomaPath, StandardCopyOption.REPLACE_EXISTING);

        DoctorVerification verification = new DoctorVerification();
        verification.setDoctorId(doctorId);
        verification.setCV(cvPath.toString());
        verification.setDiploma(diplomaPath.toString());
        verification.setLicenseNumber(licenseNumber);
        verification.setNationalId(nationalId);
        verification.setStatus(DoctorVerification.Status.PENDING);
        verification.setSubmittedAt(LocalDateTime.now());

        System.out.println("UPLOAD PATH = " + uploadPath);

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
