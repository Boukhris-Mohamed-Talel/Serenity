package com.example.healthcare.service;

import com.example.healthcare.entity.Doctor;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
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
    public Doctor createDoctorForExistingUser(Long userId, String speciality, MultipartFile image) throws IOException {

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!existingUser.getRole().name().equals("DOCTOR")) {
            throw new RuntimeException("User is not a doctor");
        }

        // Save the file
        String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
        String uploadDir = "uploads/";

        Path filePath = Paths.get(uploadDir + fileName);
        Files.createDirectories(filePath.getParent());
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create doctor and set DB fields
        Doctor doctor = new Doctor();
        doctor.setUser(existingUser);
        doctor.setSpecialty(speciality);
        doctor.setProfilePictureUrl("uploads/" + fileName);

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
    public Doctor updateDoctor(Long userId, Doctor doctorDetails) {
        Doctor existingDoctor = doctorRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (doctorDetails.getSpecialty() != null) {
            existingDoctor.setSpecialty(doctorDetails.getSpecialty());
        }

        if (doctorDetails.getProfilePictureUrl() != null) {
            existingDoctor.setProfilePictureUrl(doctorDetails.getProfilePictureUrl());
        }

        return doctorRepository.save(existingDoctor);
    }

    @Override
    public void deleteDoctor(Long id) {
        doctorRepository.deleteById(id);
    }
}