package tn.esprit.arctic.derbelmicroservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionMedicationRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionMedicationResponseDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;
import tn.esprit.arctic.derbelmicroservice.entity.value.PrescriptionMedication;

import java.util.ArrayList;
import java.util.List;

@Component
public class PrescriptionMapper {

    public PrescriptionResponseDTO toResponseDTO(Prescription prescription) {
        return PrescriptionResponseDTO.builder()
                .id(prescription.getId())
                .medications(toMedicationResponses(prescription.getMedications()))
                .medicalRecordId(prescription.getMedicalRecord().getId())
                .patientId(prescription.getPatientId())
                .doctorId(prescription.getDoctorId())
                .createdAt(prescription.getCreatedAt())
                .updatedAt(prescription.getUpdatedAt())
                .build();
    }

    public Prescription toEntity(PrescriptionRequestDTO dto, MedicalRecord medicalRecord) {
        Prescription prescription = Prescription.builder()
                .medications(toMedicationValues(dto.getMedications()))
                .medicalRecord(medicalRecord)
                .patientId(dto.getPatientId())
                .build();
        applyDerivedColumns(prescription);
        return prescription;
    }

    public void updateEntityFromDTO(PrescriptionRequestDTO dto, Prescription prescription) {
        prescription.setMedications(toMedicationValues(dto.getMedications()));
        applyDerivedColumns(prescription);
    }

    private List<PrescriptionMedication> toMedicationValues(List<PrescriptionMedicationRequestDTO> rawItems) {
        if (rawItems == null) {
            throw new IllegalArgumentException("Au moins un médicament est obligatoire");
        }

        List<PrescriptionMedication> cleaned = rawItems.stream()
                .map(this::toMedicationValue)
                .toList();

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Au moins un médicament est obligatoire");
        }

        return new ArrayList<>(cleaned);
    }

    private PrescriptionMedication toMedicationValue(PrescriptionMedicationRequestDTO item) {
        if (item == null || item.getMedicationName() == null || item.getMedicationName().isBlank()) {
            throw new IllegalArgumentException("Le nom du médicament est obligatoire");
        }
        return PrescriptionMedication.builder()
                .medicationName(item.getMedicationName().trim())
                .dosage(item.getDosage() != null ? item.getDosage().trim() : "")
                .frequency(item.getFrequency() != null ? item.getFrequency().trim() : "")
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .instructions(item.getInstructions() != null ? item.getInstructions().trim() : null)
                .quantity(item.getQuantity())
                .status(item.getStatus() != null ? item.getStatus().trim() : "")
                .build();
    }

    private List<PrescriptionMedicationResponseDTO> toMedicationResponses(List<PrescriptionMedication> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> PrescriptionMedicationResponseDTO.builder()
                        .medicationName(item.getMedicationName())
                        .dosage(item.getDosage())
                        .frequency(item.getFrequency())
                        .startDate(item.getStartDate())
                        .endDate(item.getEndDate())
                        .instructions(item.getInstructions())
                        .quantity(item.getQuantity())
                        .status(item.getStatus())
                        .build())
                .toList();
    }

    private void applyDerivedColumns(Prescription prescription) {
        List<PrescriptionMedication> meds = prescription.getMedications();
        if (meds == null || meds.isEmpty()) {
            throw new IllegalArgumentException("Au moins un médicament est obligatoire");
        }

        PrescriptionMedication first = meds.get(0);
        String namesSummary = meds.stream()
                .map(PrescriptionMedication::getMedicationName)
                .map(name -> name == null ? "" : name.trim())
                .filter(name -> !name.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(first.getMedicationName());
        if (namesSummary.length() > 255) {
            namesSummary = namesSummary.substring(0, 255);
        }

        prescription.setMedicationName(namesSummary);
        prescription.setDosage(first.getDosage() != null ? first.getDosage() : "");
        prescription.setFrequency(first.getFrequency() != null ? first.getFrequency() : "");
        prescription.setStartDate(first.getStartDate());
        prescription.setEndDate(first.getEndDate());
        prescription.setInstructions(first.getInstructions());
        prescription.setQuantity(first.getQuantity());
        prescription.setStatus(first.getStatus() != null ? first.getStatus() : "ACTIVE");
    }
}
