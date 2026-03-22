package tn.esprit.arctic.derbelmicroservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.arctic.derbelmicroservice.dto.request.PrescriptionRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;

import java.util.List;

public interface IPrescriptionService {

    Page<PrescriptionResponseDTO> getAllPrescriptions(Pageable pageable);

    PrescriptionResponseDTO getPrescriptionById(Long id);

    List<PrescriptionResponseDTO> getPrescriptionsByRecordId(Long recordId);

    PrescriptionResponseDTO createPrescription(PrescriptionRequestDTO requestDTO);

    PrescriptionResponseDTO updatePrescription(Long id, PrescriptionRequestDTO requestDTO);

    void deletePrescription(Long id);
}
