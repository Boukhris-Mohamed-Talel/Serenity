package tn.esprit.arctic.derbelmicroservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.arctic.derbelmicroservice.dto.request.PatientRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PatientResponseDTO;

public interface IPatientService {

    Page<PatientResponseDTO> getAllPatientsByDoctor(Long doctorId, Pageable pageable, boolean isAdmin);

    PatientResponseDTO getPatientById(Long id, Long doctorId, boolean isAdmin);

    PatientResponseDTO createPatient(PatientRequestDTO requestDTO, Long doctorId);

    PatientResponseDTO updatePatient(Long id, PatientRequestDTO requestDTO, Long doctorId, boolean isAdmin);

    void deletePatient(Long id, Long doctorId, boolean isAdmin);
}
