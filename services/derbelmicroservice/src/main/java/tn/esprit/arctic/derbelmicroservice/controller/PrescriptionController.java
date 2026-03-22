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
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PageResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.service.IPrescriptionService;

import java.util.List;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final IPrescriptionService prescriptionService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<PrescriptionResponseDTO>>> getAllPrescriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PrescriptionResponseDTO> resultPage = prescriptionService.getAllPrescriptions(pageable);
        return ResponseEntity.ok(ApiResponseDTO.<PageResponseDTO<PrescriptionResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Liste des prescriptions récupérée avec succès")
                .data(PageResponseDTO.of(resultPage))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<PrescriptionResponseDTO>> getPrescriptionById(@PathVariable Long id) {
        PrescriptionResponseDTO prescription = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(ApiResponseDTO.<PrescriptionResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Prescription récupérée avec succès")
                .data(prescription)
                .build());
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<ApiResponseDTO<List<PrescriptionResponseDTO>>> getPrescriptionsByRecordId(
            @PathVariable Long recordId) {
        List<PrescriptionResponseDTO> prescriptions = prescriptionService.getPrescriptionsByRecordId(recordId);
        return ResponseEntity.ok(ApiResponseDTO.<List<PrescriptionResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Prescriptions du dossier médical récupérées avec succès")
                .data(prescriptions)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<PrescriptionResponseDTO>> createPrescription(
            @Valid @RequestBody PrescriptionRequestDTO requestDTO) {
        PrescriptionResponseDTO created = prescriptionService.createPrescription(requestDTO);
        return new ResponseEntity<>(ApiResponseDTO.<PrescriptionResponseDTO>builder()
                .status(HttpStatus.CREATED.value())
                .message("Prescription créée avec succès")
                .data(created)
                .build(), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<PrescriptionResponseDTO>> updatePrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRequestDTO requestDTO) {
        PrescriptionResponseDTO updated = prescriptionService.updatePrescription(id, requestDTO);
        return ResponseEntity.ok(ApiResponseDTO.<PrescriptionResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Prescription mise à jour avec succès")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Prescription supprimée avec succès")
                .build());
    }
}
