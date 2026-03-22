package tn.esprit.arctic.derbelmicroservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.arctic.derbelmicroservice.dto.request.PatientRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PatientResponseDTO;

public interface IPatientService {

    Page<PatientResponseDTO> getAllPatients(Pageable pageable);

    PatientResponseDTO getPatientById(Long id);

    PatientResponseDTO createPatient(PatientRequestDTO requestDTO);

    PatientResponseDTO updatePatient(Long id, PatientRequestDTO requestDTO);

    void deletePatient(Long id);
}
