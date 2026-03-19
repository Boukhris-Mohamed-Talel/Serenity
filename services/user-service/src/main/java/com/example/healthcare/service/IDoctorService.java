package com.example.healthcare.service;

import com.example.healthcare.entity.Doctor;

import java.util.List;
import java.util.Optional;

public interface IDoctorService {
    Doctor createDoctorForExistingUser(Long userId, Doctor doctorDetails);

    List<Doctor> getAllDoctors();

    Optional<Doctor> getDoctorById(Long id);

    Doctor updateDoctor(Long userId, Doctor doctorDetails);

    void deleteDoctor(Long id);
}
