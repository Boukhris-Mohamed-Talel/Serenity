package serenity.doctors_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import serenity.doctors_service.entity.DoctorVerification;
import serenity.doctors_service.service.IDoctorVerificationService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/doctor-verifications")
public class DoctorVerificationController {

    @Autowired
    private IDoctorVerificationService service;

    // Create
    @PostMapping("/add_verification")
    public DoctorVerification create(@RequestBody DoctorVerification verification) {
        return service.save(verification);
    }

    // Update
    @PutMapping("/update_verification/{id}")
    public DoctorVerification update(@PathVariable("id") Long id, @RequestBody DoctorVerification verification) {
        verification.setVerification_id(id);
        return service.save(verification);
    }

    // Get all
    @GetMapping
    public List<DoctorVerification> findAll() {
        return service.findAll();
    }

    // Get by ID
    @GetMapping("/{id}")
    public Optional<DoctorVerification> findById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    // Delete by ID
    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable("id") Long id) {
        service.deleteById(id);
    }
}