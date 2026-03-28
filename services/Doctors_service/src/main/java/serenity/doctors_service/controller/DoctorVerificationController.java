package serenity.doctors_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.service.IDoctorVerificationService;
import serenity.doctors_service.service.RedisPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctor-verifications")
public class DoctorVerificationController {

    @Autowired
    private IDoctorVerificationService service;
    private final String uploadDir = "uploads/";

    @Autowired
    private RedisPublisher redisPublisher;

    // Create verification with files
    @PostMapping("/add_verification")
    public ResponseEntity<DoctorVerification> create(
            @RequestParam("cv") MultipartFile cv,
            @RequestParam("diploma") MultipartFile diploma,
            @RequestParam("licenseNumber") String licenseNumber,
            @RequestParam("nationalId") String nationalId,
            @RequestHeader("X-User-Id") String userIdHeader
    ) throws IOException {

        Long doctorId = Long.parseLong(userIdHeader);

        DoctorVerification verification = service.saveVerification(
                doctorId, cv, diploma, licenseNumber, nationalId
        );

        return ResponseEntity.ok(verification);
    }

    // Update verification
    @PutMapping("/update_verification/{id}")
    public ResponseEntity<DoctorVerification> update(
            @PathVariable Long id,
            @RequestParam(value = "cv", required = false) MultipartFile cv,
            @RequestParam(value = "diploma", required = false) MultipartFile diploma,
            @RequestParam("licenseNumber") String licenseNumber,
            @RequestParam("nationalId") String nationalId,
            @RequestHeader("X-User-Id") String userIdHeader
    ) throws IOException {

        Long doctorId = Long.parseLong(userIdHeader);

        // Load existing verification from DB
        List<DoctorVerification> list = service.findById(id);
        if (list.isEmpty()) {
            throw new RuntimeException("Verification not found");
        }
        DoctorVerification verification = list.get(0);


        // Update fields
        verification.setLicenseNumber(licenseNumber);
        verification.setNationalId(nationalId);

        // Handle file uploads
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        if (cv != null) {
            String cvFileName = System.currentTimeMillis() + "_" + cv.getOriginalFilename();
            Path cvPath = uploadPath.resolve(cvFileName);
            Files.copy(cv.getInputStream(), cvPath, StandardCopyOption.REPLACE_EXISTING);
            verification.setCV(cvPath.toString());
        }

        if (diploma != null) {
            String diplomaFileName = System.currentTimeMillis() + "_" + diploma.getOriginalFilename();
            Path diplomaPath = uploadPath.resolve(diplomaFileName);
            Files.copy(diploma.getInputStream(), diplomaPath, StandardCopyOption.REPLACE_EXISTING);
            verification.setDiploma(diplomaPath.toString());
        }

        DoctorVerification updated = service.save(verification);

        redisPublisher.publishVerification(updated);

        return ResponseEntity.ok(updated);
    }

    // Get all verifications
    @GetMapping
    public ResponseEntity<List<DoctorVerification>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // Get verification by ID
    @GetMapping("/{id}")
    public ResponseEntity<List<DoctorVerification>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("FindByDoctorID/{id}")
    public ResponseEntity<List<DoctorVerification>> findByDoctorID(@PathVariable Long id) {
        return ResponseEntity.ok(service.findByDoctorId(id));
    }

    // Delete verification by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("Approve/{id}")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        service.Approve(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("Reject/{id}")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        service.Reject(id);
        return ResponseEntity.noContent().build();
    }
}