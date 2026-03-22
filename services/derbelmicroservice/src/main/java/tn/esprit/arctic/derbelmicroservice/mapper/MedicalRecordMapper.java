package tn.esprit.arctic.derbelmicroservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicalRecordRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicalRecordResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.Patient;

@Component
public class MedicalRecordMapper {

    /**
     * Utilise le patient déjà chargé (création / mise à jour) pour éviter tout accès lazy
     * hors session avec {@code spring.jpa.open-in-view=false}.
     */
    public MedicalRecordResponseDTO toResponseDTO(MedicalRecord record, Patient patient) {
        return MedicalRecordResponseDTO.builder()
                .id(record.getId())
                .diagnosis(record.getDiagnosis())
                .notes(record.getNotes())
                .date(record.getDate())
                .severity(record.getSeverity())
                .status(record.getStatus())
                .patientId(patient.getId())
                .patientFirstName(patient.getFirstName())
                .patientLastName(patient.getLastName())
                .doctorId(record.getDoctorId())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    public MedicalRecordResponseDTO toResponseDTO(MedicalRecord record) {
        Patient p = record.getPatient();
        return MedicalRecordResponseDTO.builder()
                .id(record.getId())
                .diagnosis(record.getDiagnosis())
                .notes(record.getNotes())
                .date(record.getDate())
                .severity(record.getSeverity())
                .status(record.getStatus())
                .patientId(p.getId())
                .patientFirstName(p.getFirstName())
                .patientLastName(p.getLastName())
                .doctorId(record.getDoctorId())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    public MedicalRecord toEntity(MedicalRecordRequestDTO dto, Patient patient) {
        return MedicalRecord.builder()
                .diagnosis(dto.getDiagnosis())
                .notes(dto.getNotes())
                .date(dto.getDate())
                .severity(dto.getSeverity())
                .status(dto.getStatus())
                .patient(patient)
                .doctorId(dto.getDoctorId())
                .build();
    }

    public void updateEntityFromDTO(MedicalRecordRequestDTO dto, MedicalRecord record) {
        record.setDiagnosis(dto.getDiagnosis());
        record.setNotes(dto.getNotes());
        record.setDate(dto.getDate());
        record.setSeverity(dto.getSeverity());
        record.setStatus(dto.getStatus());
        record.setDoctorId(dto.getDoctorId());
    }
}
