package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arctic.derbelmicroservice.dto.request.PatientRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PatientResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.Patient;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.PatientMapper;
import tn.esprit.arctic.derbelmicroservice.repository.PatientRepository;
import tn.esprit.arctic.derbelmicroservice.service.IPatientService;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements IPatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponseDTO> getAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable)
                .map(patientMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        return patientMapper.toResponseDTO(patient);
    }

    @Override
    public PatientResponseDTO createPatient(PatientRequestDTO requestDTO) {
        Patient patient = patientMapper.toEntity(requestDTO);
        Patient saved = patientRepository.save(patient);
        return patientMapper.toResponseDTO(saved);
    }

    @Override
    public PatientResponseDTO updatePatient(Long id, PatientRequestDTO requestDTO) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        patientMapper.updateEntityFromDTO(requestDTO, patient);
        Patient updated = patientRepository.save(patient);
        return patientMapper.toResponseDTO(updated);
    }

    @Override
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        patientRepository.delete(patient);
    }
}
