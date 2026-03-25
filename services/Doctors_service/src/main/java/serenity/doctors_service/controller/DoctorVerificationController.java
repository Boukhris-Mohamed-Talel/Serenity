package serenity.doctors_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.service.IDoctorVerificationService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctor-verifications")
public class DoctorVerificationController {

    @Autowired
    private IDoctorVerificationService service;

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
            @RequestBody DoctorVerification verification
    ) {
        verification.setVerification_id(id);
        DoctorVerification updated = service.save(verification);
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
}