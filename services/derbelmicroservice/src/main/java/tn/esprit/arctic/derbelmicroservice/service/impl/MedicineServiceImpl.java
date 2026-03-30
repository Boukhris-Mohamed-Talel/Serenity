package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arctic.derbelmicroservice.dto.request.MedicineRequestDTO;
import tn.esprit.arctic.derbelmicroservice.dto.response.MedicineResponseDTO;
import tn.esprit.arctic.derbelmicroservice.entity.Medicine;
import tn.esprit.arctic.derbelmicroservice.exception.ResourceNotFoundException;
import tn.esprit.arctic.derbelmicroservice.mapper.MedicineMapper;
import tn.esprit.arctic.derbelmicroservice.repository.MedicineRepository;
import tn.esprit.arctic.derbelmicroservice.service.IMedicineService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicineServiceImpl implements IMedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineMapper medicineMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponseDTO> getAllMedicines() {
        return medicineRepository.findAll().stream()
                .map(medicineMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponseDTO> searchMedicines(String name) {
        return medicineRepository.findByNameContainingIgnoreCase(name).stream()
                .map(medicineMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicineResponseDTO getMedicineById(Long id) {
        return medicineMapper.toResponseDTO(findOrThrow(id));
    }

    @Override
    public MedicineResponseDTO createMedicine(MedicineRequestDTO dto) {
        if (medicineRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new IllegalArgumentException("Un médicament avec ce nom existe déjà: " + dto.getName());
        }
        Medicine entity = medicineMapper.toEntity(dto);
        return medicineMapper.toResponseDTO(medicineRepository.save(entity));
    }

    @Override
    public MedicineResponseDTO updateMedicine(Long id, MedicineRequestDTO dto) {
        Medicine entity = findOrThrow(id);
        medicineRepository.findByNameIgnoreCase(dto.getName().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Un médicament avec ce nom existe déjà: " + dto.getName());
                });
        medicineMapper.updateEntity(dto, entity);
        return medicineMapper.toResponseDTO(medicineRepository.save(entity));
    }

    @Override
    public void deleteMedicine(Long id) {
        Medicine entity = findOrThrow(id);
        medicineRepository.delete(entity);
    }

    private Medicine findOrThrow(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", "id", id));
    }
}
