package com.example.healthcare.service;

import com.example.healthcare.dto.DoctorUpdateRequest;
import com.example.healthcare.entity.Doctor;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
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


    public Doctor updateDoctorFull(Long id, DoctorUpdateRequest request) throws IOException {

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // --- Update User fields ---
        if (request.getFirstName() != null)   doctor.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    doctor.setLastName(request.getLastName());
        if (request.getPhone() != null)       doctor.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) doctor.setDateOfBirth(request.getDateOfBirth());

        // --- Update UserProfile fields ---
        UserProfile profile = doctor.getProfile();
        if (profile == null) {
            profile = UserProfile.builder()
                    .user(doctor)
                    .build();
        }
        if (request.getAvatarUrl() != null)        profile.setAvatar(request.getAvatarUrl());
        if (request.getBio() != null)              profile.setBio(request.getBio());
        if (request.getPreferredLanguage() != null) profile.setPreferredLanguage(request.getPreferredLanguage());
        if (request.getIsAnonymous() != null)      profile.setIsAnonymous(request.getIsAnonymous());
        doctor.setProfile(profile);

        // --- Update Doctor fields ---
        if (request.getSpecialty() != null) doctor.setSpecialty(request.getSpecialty());

        // --- Handle profile picture file upload ---
        MultipartFile image = request.getImage();
        if (image != null && !image.isEmpty()) {
            Files.createDirectories(Paths.get("uploads"));
            String safeName = image.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = UUID.randomUUID() + "-" + safeName;
            Path filePath = Paths.get("uploads/" + filename);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            doctor.setProfilePictureUrl("uploads/" + filename);
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