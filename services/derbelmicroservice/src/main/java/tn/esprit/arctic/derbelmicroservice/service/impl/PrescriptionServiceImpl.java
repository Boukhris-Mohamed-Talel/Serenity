package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.MedicalRecord;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.PrescriptionMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicalRecordRepository;
import tn.esprit.arctic.derbelmicroservice.repository.PrescriptionRepository;
import tn.esprit.arctic.derbelmicroservice.service.IPrescriptionService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements IPrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionMapper prescriptionMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getAllPrescriptions(Pageable pageable) {
        return prescriptionRepository.findAll(pageable)
                .map(prescriptionMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescriptionById(Long id) {
        Prescription prescription = prescriptionRepository.findByIdWithMedicalRecord(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
        return prescriptionMapper.toResponseDTO(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getPrescriptionsByRecordId(Long recordId) {
        if (!medicalRecordRepository.existsById(recordId)) {
            throw new ResourceNotFoundException("MedicalRecord", "id", recordId);
        }
        return prescriptionRepository.findByMedicalRecordId(recordId)
                .stream()
                .map(prescriptionMapper::toResponseDTO)
                .toList();
    }

    @Override
    public PrescriptionResponseDTO createPrescription(PrescriptionRequestDTO requestDTO) {
        MedicalRecord record = medicalRecordRepository.findById(requestDTO.getMedicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", requestDTO.getMedicalRecordId()));
        Prescription prescription = prescriptionMapper.toEntity(requestDTO, record);
        Prescription saved = prescriptionRepository.save(prescription);
        return prescriptionMapper.toResponseDTO(saved);
    }

    @Override
    public PrescriptionResponseDTO updatePrescription(Long id, PrescriptionRequestDTO requestDTO) {
        Prescription prescription = prescriptionRepository.findByIdWithMedicalRecord(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
        if (requestDTO.getMedicalRecordId() != null
                && !requestDTO.getMedicalRecordId().equals(prescription.getMedicalRecord().getId())) {
            MedicalRecord newRecord = medicalRecordRepository.findById(requestDTO.getMedicalRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", requestDTO.getMedicalRecordId()));
            prescription.setMedicalRecord(newRecord);
        }
        prescriptionMapper.updateEntityFromDTO(requestDTO, prescription);
        Prescription updated = prescriptionRepository.save(prescription);
        return prescriptionMapper.toResponseDTO(updated);
    }

    @Override
    public void deletePrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
        prescriptionRepository.delete(prescription);
    }
}
