package com.example.healthcare.service;

import com.example.healthcare.entity.Doctor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IDoctorService {
    Doctor createDoctorForExistingUser(Long userId, String speciality, MultipartFile image)throws IOException;

    List<Doctor> getAllDoctors();

    Optional<Doctor> getDoctorById(Long id);

    Doctor updateDoctor(Long userId, Doctor doctorDetails);

    void deleteDoctor(Long id);

    void Verify(Long id);
}
