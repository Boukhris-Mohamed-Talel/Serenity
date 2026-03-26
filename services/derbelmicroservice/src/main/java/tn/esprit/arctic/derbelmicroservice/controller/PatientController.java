package tn.esprit.arctic.derbelmicroservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.arctic.derbelmicroservice.dto.request.PatientRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PageResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PatientResponseDTO;
import tn.esprit.arctic.derbelmicroservice.security.DerbelAuth;
import tn.esprit.arctic.derbelmicroservice.service.IPatientService;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final IPatientService patientService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<PatientResponseDTO>>> getAllPatients(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Long effectiveDoctorId = isAdmin ? doctorId : authenticatedUserId;
        Page<PatientResponseDTO> resultPage = patientService.getAllPatientsByDoctor(effectiveDoctorId, pageable, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<PageResponseDTO<PatientResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Liste des patients récupérée avec succès")
                .data(PageResponseDTO.of(resultPage))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<PatientResponseDTO>> getPatientById(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        PatientResponseDTO patient = patientService.getPatientById(id, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<PatientResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Patient récupéré avec succès")
                .data(patient)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<PatientResponseDTO>> createPatient(@Valid @RequestBody PatientRequestDTO requestDTO) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();

        PatientResponseDTO created = patientService.createPatient(requestDTO, authenticatedUserId);
        return new ResponseEntity<>(ApiResponseDTO.<PatientResponseDTO>builder()
                .status(HttpStatus.CREATED.value())
                .message("Patient créé avec succès")
                .data(created)
                .build(), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<PatientResponseDTO>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequestDTO requestDTO) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        PatientResponseDTO updated = patientService.updatePatient(id, requestDTO, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<PatientResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Patient mis à jour avec succès")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deletePatient(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        Long authenticatedUserId = DerbelAuth.requireUserId();
        boolean isAdmin = DerbelAuth.isAdmin();

        patientService.deletePatient(id, authenticatedUserId, isAdmin);
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Patient supprimé avec succès")
                .build());
    }
}
