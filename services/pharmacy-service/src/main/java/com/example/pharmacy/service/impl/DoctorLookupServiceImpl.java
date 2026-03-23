package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.DoctorMedicineSuggestionItemDTO;
import com.example.pharmacy.dto.DoctorMedicineSuggestionResponseDTO;
import com.example.pharmacy.entity.MedicineStockItem;
import com.example.pharmacy.entity.PatientPharmacyPreference;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PatientPharmacyPreferenceRepository;
import com.example.pharmacy.service.DoctorLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorLookupServiceImpl implements DoctorLookupService {

    private static final String NO_DEFAULT_PHARMACY_MESSAGE = "Patient has no default pharmacy yet";

    private final PatientPharmacyPreferenceRepository preferenceRepository;
    private final MedicineStockItemRepository medicineStockItemRepository;

    @Override
    public DoctorMedicineSuggestionResponseDTO suggestMedicines(Long patientId, String query) {
        String normalizedQuery = normalizeQuery(query);
        PatientPharmacyPreference preference = preferenceRepository.findByPatientId(patientId).orElse(null);

        if (preference == null || preference.getDefaultPharmacy() == null) {
            return DoctorMedicineSuggestionResponseDTO.builder()
                .patientId(patientId)
                .hasDefaultPharmacy(false)
                .pharmacyId(null)
                .pharmacyName(null)
                .guidanceMessage(NO_DEFAULT_PHARMACY_MESSAGE)
                .suggestions(buildUnresolvedSuggestions(normalizedQuery))
                .build();
        }

        Pharmacy pharmacy = preference.getDefaultPharmacy();
        return DoctorMedicineSuggestionResponseDTO.builder()
            .patientId(patientId)
            .hasDefaultPharmacy(true)
            .pharmacyId(pharmacy.getId())
            .pharmacyName(pharmacy.getName())
            .guidanceMessage(null)
            .suggestions(buildScopedSuggestions(pharmacy.getId(), normalizedQuery))
            .build();
    }

    private List<DoctorMedicineSuggestionItemDTO> buildScopedSuggestions(Long pharmacyId, String query) {
        List<MedicineStockItem> stockItems = medicineStockItemRepository.findForDoctorSuggestionInPharmacy(pharmacyId, query);
        Map<String, Integer> quantityByMedicine = new LinkedHashMap<>();

        for (MedicineStockItem item : stockItems) {
            String key = item.getMedicineName().trim();
            quantityByMedicine.putIfAbsent(key, 0);
            quantityByMedicine.put(key, quantityByMedicine.get(key) + Math.max(item.getQuantity(), 0));
        }

        List<DoctorMedicineSuggestionItemDTO> suggestions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantityByMedicine.entrySet()) {
            boolean inStock = entry.getValue() > 0;
            suggestions.add(DoctorMedicineSuggestionItemDTO.builder()
                .medicineName(entry.getKey())
                .stockStatus(inStock ? "IN_STOCK" : "OUT_OF_STOCK")
                .availableQuantity(entry.getValue())
                .guidanceMessage(inStock ? null : "Currently out of stock in patient's default pharmacy")
                .build());

            if (suggestions.size() >= 20) {
                break;
            }
        }

        return suggestions;
    }

    private List<DoctorMedicineSuggestionItemDTO> buildUnresolvedSuggestions(String query) {
        List<String> names = medicineStockItemRepository.findDistinctMedicineNamesForSuggestion(query);
        List<DoctorMedicineSuggestionItemDTO> suggestions = new ArrayList<>();

        for (String name : names) {
            suggestions.add(DoctorMedicineSuggestionItemDTO.builder()
                .medicineName(name)
                .stockStatus("UNRESOLVED")
                .availableQuantity(null)
                .guidanceMessage(NO_DEFAULT_PHARMACY_MESSAGE)
                .build());

            if (suggestions.size() >= 20) {
                break;
            }
        }

        return suggestions;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query is required");
        }

        return query.trim();
    }
}
