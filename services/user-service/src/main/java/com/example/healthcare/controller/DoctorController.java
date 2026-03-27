package com.example.healthcare.controller;

import com.example.healthcare.entity.Doctor;
import com.example.healthcare.service.DoctorService;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateDoctor(
            @PathVariable Long userId,
            @RequestBody Doctor doctorDetails) {

        Doctor updatedDoctor = doctorService.updateDoctor(userId, doctorDetails);

        // 👇 retourne un DTO simple au lieu de l'entité complète
        Map<String, Object> response = new HashMap<>();
        response.put("id", updatedDoctor.getId());
        response.put("specialty", updatedDoctor.getSpecialty());
        response.put("profilePictureUrl", updatedDoctor.getProfilePictureUrl());

        return ResponseEntity.ok(response);
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