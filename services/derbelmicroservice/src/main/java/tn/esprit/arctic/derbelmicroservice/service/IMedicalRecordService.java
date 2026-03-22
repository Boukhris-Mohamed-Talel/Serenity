package tn.esprit.arctic.derbelmicroservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicalRecordRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicalRecordResponseDTO;

import java.util.List;

public interface IMedicalRecordService {

    Page<MedicalRecordResponseDTO> getAllRecords(Pageable pageable);

    MedicalRecordResponseDTO getRecordById(Long id);

    List<MedicalRecordResponseDTO> getRecordsByPatientId(Long patientId);

    MedicalRecordResponseDTO createRecord(MedicalRecordRequestDTO requestDTO);

    MedicalRecordResponseDTO updateRecord(Long id, MedicalRecordRequestDTO requestDTO);

    void deleteRecord(Long id);
}
