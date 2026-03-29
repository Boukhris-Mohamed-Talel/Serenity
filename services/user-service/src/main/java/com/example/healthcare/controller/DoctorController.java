package com.example.healthcare.controller;

import com.example.healthcare.dto.DoctorUpdateRequest;
import com.example.healthcare.entity.Doctor;
import com.example.healthcare.service.DoctorService;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    // Create doctor profile for an existing user
    @PostMapping("/{userId}")
    @PermitAll
    public ResponseEntity<Doctor> createDoctorForExistingUser(
            @PathVariable Long userId,
            @RequestParam("speciality") String speciality,
            @RequestParam("image") MultipartFile image
    ) throws IOException {


        Doctor doctor = doctorService.createDoctorForExistingUser(userId, speciality, image);

        return ResponseEntity.ok(doctor);
    }

    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Long id) {
        return doctorService.getDoctorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Doctor> updateDoctor(
            @PathVariable Long id,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateOfBirth,
            @RequestParam(required = false) String avatarUrl,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String preferredLanguage,
            @RequestParam(required = false) Boolean isAnonymous,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) MultipartFile image
    ) throws IOException {

        DoctorUpdateRequest request = DoctorUpdateRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .dateOfBirth(dateOfBirth)
                .avatarUrl(avatarUrl)
                .bio(bio)
                .preferredLanguage(preferredLanguage)
                .isAnonymous(isAnonymous)
                .specialty(specialty)
                .image(image)
                .build();

        Doctor updated = doctorService.updateDoctorFull(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("VerifyDoctor/{id}")
    public ResponseEntity<Doctor> verifyDoctor(@PathVariable Long id){
        doctorService.Verify(id);
        return ResponseEntity.ok().build();
    }
}