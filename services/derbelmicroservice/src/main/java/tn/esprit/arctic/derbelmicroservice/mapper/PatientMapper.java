package tn.esprit.arctic.derbelmicroservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.arctic.derbelmicroservice.dto.request.PatientRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PatientResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.Patient;

@Component
public class PatientMapper {

    public PatientResponseDTO toResponseDTO(Patient patient) {
        return PatientResponseDTO.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .bloodType(patient.getBloodType())
                .allergies(patient.getAllergies())
                .phone(patient.getPhone())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }

    public Patient toEntity(PatientRequestDTO dto) {
        return Patient.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .bloodType(dto.getBloodType())
                .allergies(dto.getAllergies())
                .phone(dto.getPhone())
                .build();
    }

    public void updateEntityFromDTO(PatientRequestDTO dto, Patient patient) {
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setGender(dto.getGender());
        patient.setBloodType(dto.getBloodType());
        patient.setAllergies(dto.getAllergies());
        patient.setPhone(dto.getPhone());
    }
}
