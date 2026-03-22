package tn.esprit.arctic.derbelmicroservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;

@Component
public class PrescriptionMapper {

    public PrescriptionResponseDTO toResponseDTO(Prescription prescription) {
        return PrescriptionResponseDTO.builder()
                .id(prescription.getId())
                .medicationName(prescription.getMedicationName())
                .dosage(prescription.getDosage())
                .frequency(prescription.getFrequency())
                .startDate(prescription.getStartDate())
                .endDate(prescription.getEndDate())
                .instructions(prescription.getInstructions())
                .quantity(prescription.getQuantity())
                .status(prescription.getStatus())
                .medicalRecordId(prescription.getMedicalRecord().getId())
                .patientId(prescription.getPatientId())
                .doctorId(prescription.getDoctorId())
                .createdAt(prescription.getCreatedAt())
                .updatedAt(prescription.getUpdatedAt())
                .build();
    }

    public Prescription toEntity(PrescriptionRequestDTO dto, MedicalRecord medicalRecord) {
        return Prescription.builder()
                .medicationName(dto.getMedicationName())
                .dosage(dto.getDosage())
                .frequency(dto.getFrequency())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .instructions(dto.getInstructions())
                .quantity(dto.getQuantity())
                .status(dto.getStatus())
                .medicalRecord(medicalRecord)
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .build();
    }

    public void updateEntityFromDTO(PrescriptionRequestDTO dto, Prescription prescription) {
        prescription.setMedicationName(dto.getMedicationName());
        prescription.setDosage(dto.getDosage());
        prescription.setFrequency(dto.getFrequency());
        prescription.setStartDate(dto.getStartDate());
        prescription.setEndDate(dto.getEndDate());
        prescription.setInstructions(dto.getInstructions());
        prescription.setQuantity(dto.getQuantity());
        prescription.setStatus(dto.getStatus());
        prescription.setDoctorId(dto.getDoctorId());
    }
}
