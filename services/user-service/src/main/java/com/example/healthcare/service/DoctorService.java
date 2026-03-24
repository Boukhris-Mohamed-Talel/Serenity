package com.example.healthcare.service;

import com.example.healthcare.entity.Doctor;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.DoctorRepository;
import com.example.healthcare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorService implements IDoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

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
        doctor.setIsActive(existingUser.getIsActive());

        // Doctor-specific fields
        doctor.setSpecialty(specialty);
        doctor.setProfilePictureUrl("uploads/" + fileName);

        // Delete the plain User row and save as Doctor (joined table)
        userRepository.delete(existingUser);
        return doctorRepository.save(doctor);
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
}