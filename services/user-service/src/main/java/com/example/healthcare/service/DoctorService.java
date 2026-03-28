package com.example.healthcare.service;

import com.example.healthcare.entity.Doctor;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.DoctorRepository;
import com.example.healthcare.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Transient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DoctorService implements IDoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisPublisher redisPublisher;


    @Override
    public Doctor createDoctorForExistingUser(Long userId, String specialty, MultipartFile image) throws IOException {

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (existingUser.getRole() != Role.DOCTOR) {
            throw new RuntimeException("User with id " + userId + " does not have the DOCTOR role");
        }

        // Save the uploaded image
        String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
        Path filePath = Paths.get("uploads/", fileName);
        Files.createDirectories(filePath.getParent());
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Build Doctor by copying all User fields (since Doctor now extends User)
        Doctor doctor = new Doctor();
        doctor.setId(existingUser.getId());
        doctor.setEmail(existingUser.getEmail());
        doctor.setPassword(existingUser.getPassword());
        doctor.setFirstName(existingUser.getFirstName());
        doctor.setLastName(existingUser.getLastName());
        doctor.setPhone(existingUser.getPhone());
        doctor.setDateOfBirth(existingUser.getDateOfBirth());
        doctor.setRole(existingUser.getRole());
        doctor.setAuthProvider(existingUser.getAuthProvider());
        doctor.setIsActive(false);

        // Doctor-specific fields
        doctor.setSpecialty(specialty);
        doctor.setProfilePictureUrl("uploads/" + fileName);
        log("=== WORKING DIR: " + System.getProperty("user.dir"));
        log("=== SAVING TO: " + Paths.get("uploads/" + fileName).toAbsolutePath());

        // Delete the plain User row and save as Doctor (joined table)
        userRepository.delete(existingUser);
        Doctor savedDoctor = doctorRepository.save(doctor);

        redisPublisher.publishDoctorEvent(savedDoctor);

        return savedDoctor;
    }

    @Override
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    @Override
    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    @Override
    public Doctor updateDoctor(Long id, Doctor doctorDetails) {
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));

        if (doctorDetails.getSpecialty() != null) {
            existingDoctor.setSpecialty(doctorDetails.getSpecialty());
        }

        if (doctorDetails.getProfilePictureUrl() != null) {
            existingDoctor.setProfilePictureUrl(doctorDetails.getProfilePictureUrl());
        }

        // Optionally allow updating base User fields too
        if (doctorDetails.getFirstName() != null) {
            existingDoctor.setFirstName(doctorDetails.getFirstName());
        }

        if (doctorDetails.getLastName() != null) {
            existingDoctor.setLastName(doctorDetails.getLastName());
        }

        if (doctorDetails.getPhone() != null) {
            existingDoctor.setPhone(doctorDetails.getPhone());
        }


        return doctorRepository.save(existingDoctor);
    }

    @Override
    public void deleteDoctor(Long id) {
        doctorRepository.deleteById(id);
    }

    @Override
    public void Verify(Long id){
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + id));

        existingDoctor.setIsActive(true);
        doctorRepository.save(existingDoctor);
    }


    public Doctor updateDoctorWithFile(Long id, String specialty, MultipartFile image) throws IOException {
        log("=== updateDoctorWithFile called ===");
        log("id: " + id);
        log("specialty: " + specialty);
        log("image is null: " + (image == null));
        log("image is empty: " + (image != null && image.isEmpty()));
        log("image original name: " + (image != null ? image.getOriginalFilename() : "N/A"));
        log("working dir: " + System.getProperty("user.dir"));

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (specialty != null) doctor.setSpecialty(specialty);

        if (image != null && !image.isEmpty()) {
            Files.createDirectories(Paths.get("uploads"));
            String safeName = image.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = UUID.randomUUID() + "-" + safeName;
            Path filePath = Paths.get("uploads/" + filename);
            log("saving to: " + filePath.toAbsolutePath());
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log("file saved ✅");
            doctor.setProfilePictureUrl("uploads/" + filename);
        } else {
            log("⚠️ image is null or empty — skipping file save");
        }

        return doctorRepository.save(doctor);
    }

    private void log(String message) {
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("debug.log"),
                    message + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {}
    }
}