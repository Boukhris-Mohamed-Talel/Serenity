package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicalRecordRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicalRecordResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.Patient;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.MedicalRecordMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicalRecordRepository;
import tn.esprit.arctic.derbelmicroservice.repository.PatientRepository;
import tn.esprit.arctic.derbelmicroservice.service.IMedicalRecordService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalRecordServiceImpl implements IMedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordMapper medicalRecordMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalRecordResponseDTO> getAllRecords(Pageable pageable) {
        return medicalRecordRepository.findAll(pageable)
                .map(medicalRecordMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponseDTO getRecordById(Long id) {
        MedicalRecord record = medicalRecordRepository.findByIdWithPatient(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", id));
        return medicalRecordMapper.toResponseDTO(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDTO> getRecordsByPatientId(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", "id", patientId);
        }
        return medicalRecordRepository.findByPatientId(patientId)
                .stream()
                .map(medicalRecordMapper::toResponseDTO)
                .toList();
    }

    @Override
    public MedicalRecordResponseDTO createRecord(MedicalRecordRequestDTO requestDTO) {
        Patient patient = patientRepository.findById(requestDTO.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", requestDTO.getPatientId()));
        MedicalRecord record = medicalRecordMapper.toEntity(requestDTO, patient);
        MedicalRecord saved = medicalRecordRepository.save(record);
        return medicalRecordMapper.toResponseDTO(saved, patient);
    }

    @Override
    public MedicalRecordResponseDTO updateRecord(Long id, MedicalRecordRequestDTO requestDTO) {
        MedicalRecord record = medicalRecordRepository.findByIdWithPatient(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", id));
        Patient patientForResponse = record.getPatient();
        if (requestDTO.getPatientId() != null && !requestDTO.getPatientId().equals(patientForResponse.getId())) {
            patientForResponse = patientRepository.findById(requestDTO.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", requestDTO.getPatientId()));
            record.setPatient(patientForResponse);
        }
        medicalRecordMapper.updateEntityFromDTO(requestDTO, record);
        MedicalRecord updated = medicalRecordRepository.save(record);
        return medicalRecordMapper.toResponseDTO(updated, patientForResponse);
    }

    @Override
    public void deleteRecord(Long id) {
        MedicalRecord record = medicalRecordRepository.findByIdWithPatient(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", id));
        medicalRecordRepository.delete(record);
    }
}
