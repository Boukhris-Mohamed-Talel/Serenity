package serenity.doctors_service.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.repository.DoctorVerificationRepository;
import serenity.doctors_service.service.RedisPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DoctorVerificationService implements IDoctorVerificationService {

    @Autowired
    private DoctorVerificationRepository repository;
    private final String uploadDir = "uploads/";
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private MailService mailService;



    @Autowired
    private RedisPublisher publisher;

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
        verification.setDoctorId(doctorId + 1L);
        verification.setCV(cvPath.toString());
        verification.setDiploma(diplomaPath.toString());
        verification.setLicenseNumber(licenseNumber);
        verification.setNationalId(nationalId);
        verification.setStatus(DoctorVerification.Status.PENDING);
        verification.setSubmittedAt(LocalDateTime.now());

        System.out.println("UPLOAD PATH = " + uploadPath);

        DoctorVerification savedVerification = repository.save(verification);

        publisher.publishVerification(savedVerification);

        return savedVerification;
    }


    @Override
    public List<DoctorVerification> findAll() {
        return repository.findAll();
    }


    @Override
    public List<DoctorVerification> findById(Long verification_id) {
        return repository.findById(verification_id)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<DoctorVerification> findByDoctorId(Long id) {
        return repository.findByDoctorId(id);
    }


    @Override
    public void deleteById(Long verification_id) {
        repository.deleteById(verification_id);
    }

    @Override
    public void Approve(Long verification_id, @RequestHeader("Authorization") String authHeader){
        DoctorVerification verification = repository.findById(verification_id).get();
        Long doctor_id = verification.getDoctorId();
        String url = "http://localhost:8081/api/doctors/email?doctorId=" + doctor_id;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader); // pass the same JWT

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        String email = response.getBody();
        mailService.sendEmail(email, "Doctor Verification Approved", "AAAAAAAAAAA333333");


        /*verification.setStatus(DoctorVerification.Status.APPROVED);
        repository.save(verification);*/
    }

    @Override
    public void Reject(Long verification_id){
        DoctorVerification verification = repository.findById(verification_id).get();
        verification.setStatus(DoctorVerification.Status.REJECTED);
        repository.save(verification);
    }

    @Override
    public void testEmail(){
        mailService.sendEmail("sihaythemabdellaoui@gmail.com", "Doctor Verification Approved", "Your verification has been approved. Congratulations!");
    }


}
