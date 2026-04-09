package tn.esprit.arctic.derbelmicroservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicineRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicineResponseDTO;
import tn.esprit.arctic.derbelmicroservice.security.DerbelAuth;
import tn.esprit.arctic.derbelmicroservice.service.IMedicineService;

import java.util.List;

@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final IMedicineService medicineService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<MedicineResponseDTO>>> getAllMedicines(
            @RequestParam(required = false) String name) {
        DerbelAuth.requireDoctorOrAdmin();

        List<MedicineResponseDTO> list = (name != null && !name.isBlank())
                ? medicineService.searchMedicines(name)
                : medicineService.getAllMedicines();

        return ResponseEntity.ok(ApiResponseDTO.<List<MedicineResponseDTO>>builder()
                .status(HttpStatus.OK.value())
                .message("Liste des médicaments récupérée avec succès")
                .data(list)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<MedicineResponseDTO>> getMedicineById(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        return ResponseEntity.ok(ApiResponseDTO.<MedicineResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Médicament récupéré avec succès")
                .data(medicineService.getMedicineById(id))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<MedicineResponseDTO>> createMedicine(
            @Valid @RequestBody MedicineRequestDTO dto) {
        DerbelAuth.requireDoctorOrAdmin();
        MedicineResponseDTO created = medicineService.createMedicine(dto);
        return new ResponseEntity<>(ApiResponseDTO.<MedicineResponseDTO>builder()
                .status(HttpStatus.CREATED.value())
                .message("Médicament créé avec succès")
                .data(created)
                .build(), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<MedicineResponseDTO>> updateMedicine(
            @PathVariable Long id,
            @Valid @RequestBody MedicineRequestDTO dto) {
        DerbelAuth.requireDoctorOrAdmin();
        return ResponseEntity.ok(ApiResponseDTO.<MedicineResponseDTO>builder()
                .status(HttpStatus.OK.value())
                .message("Médicament mis à jour avec succès")
                .data(medicineService.updateMedicine(id, dto))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteMedicine(@PathVariable Long id) {
        DerbelAuth.requireDoctorOrAdmin();
        medicineService.deleteMedicine(id);
        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Médicament supprimé avec succès")
                .build());
    }
}
