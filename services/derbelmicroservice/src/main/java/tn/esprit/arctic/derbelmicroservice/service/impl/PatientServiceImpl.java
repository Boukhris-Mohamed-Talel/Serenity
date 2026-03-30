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
    public Page<PatientResponseDTO> getAllPatientsByDoctor(Long doctorId, Pageable pageable, boolean isAdmin) {
        if (isAdmin && doctorId == null) {
            return patientRepository.findAll(pageable)
                    .map(patientMapper::toResponseDTO);
        }
        return patientRepository.findByDoctorId(doctorId, pageable)
                .map(patientMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(Long id, Long doctorId, boolean isAdmin) {
        Patient patient = (isAdmin
                ? patientRepository.findById(id)
                : patientRepository.findByIdAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        return patientMapper.toResponseDTO(patient);
    }

    @Override
    public PatientResponseDTO createPatient(PatientRequestDTO requestDTO, Long doctorId) {
        Patient patient = patientMapper.toEntity(requestDTO);
        patient.setDoctorId(doctorId);
        Patient saved = patientRepository.save(patient);
        return patientMapper.toResponseDTO(saved);
    }

    @Override
    public PatientResponseDTO updatePatient(Long id, PatientRequestDTO requestDTO, Long doctorId, boolean isAdmin) {
        Patient patient = (isAdmin
                ? patientRepository.findById(id)
                : patientRepository.findByIdAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        patientMapper.updateEntityFromDTO(requestDTO, patient);
        Patient updated = patientRepository.save(patient);
        return patientMapper.toResponseDTO(updated);
    }

    @Override
    public void deletePatient(Long id, Long doctorId, boolean isAdmin) {
        Patient patient = (isAdmin
                ? patientRepository.findById(id)
                : patientRepository.findByIdAndDoctorId(id, doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        patientRepository.delete(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<PatientResponseDTO> searchPatients(String name, Long doctorId, boolean isAdmin) {
        java.util.List<Patient> results = isAdmin
                ? patientRepository.searchByName(name)
                : patientRepository.searchByNameAndDoctorId(name, doctorId);
        return results.stream().map(patientMapper::toResponseDTO).toList();
    }
}
