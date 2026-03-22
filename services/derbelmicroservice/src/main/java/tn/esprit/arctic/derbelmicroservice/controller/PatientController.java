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
import tn.esprit.arctic.derbelmicroservice.service.IPatientService;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final IPatientService patientService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<PatientResponseDTO>>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PatientResponseDTO> resultPage = patientService.getAllPatients(pageable);
        return ResponseEntity.ok(ApiResponseDTO.<PageResponseDTO<PatientResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Liste des patients récupérée avec succès")
                .data(PageResponseDTO.of(resultPage))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<PatientResponseDTO>> getPatientById(@PathVariable Long id) {
        PatientResponseDTO patient = patientService.getPatientById(id);
        return ResponseEntity.ok(ApiResponseDTO.<PatientResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Patient récupéré avec succès")
                .data(patient)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<PatientResponseDTO>> createPatient(
            @Valid @RequestBody PatientRequestDTO requestDTO) {
        PatientResponseDTO created = patientService.createPatient(requestDTO);
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
        PatientResponseDTO updated = patientService.updatePatient(id, requestDTO);
        return ResponseEntity.ok(ApiResponseDTO.<PatientResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Patient mis à jour avec succès")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Patient supprimé avec succès")
                .build());
    }
}
