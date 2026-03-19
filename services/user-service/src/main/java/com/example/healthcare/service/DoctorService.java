package com.example.healthcare.service;

import com.example.healthcare.entity.Doctor;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
import com.example.healthcare.repository.DoctorRepository;
import com.example.healthcare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorService implements IDoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Doctor createDoctorForExistingUser(Long userId, Doctor doctorDetails) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!existingUser.getRole().name().equals("DOCTOR")) {
            throw new RuntimeException("User is not a doctor");
        }

        Doctor doctor = new Doctor();
        doctor.setUser(existingUser); // link to existing user
        doctor.setSpecialty(doctorDetails.getSpecialty());
        doctor.setProfilePictureUrl(doctorDetails.getProfilePictureUrl());

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