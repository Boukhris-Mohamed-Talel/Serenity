package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.DoctorMedicineSuggestionItemDTO;
import com.example.pharmacy.dto.DoctorMedicineSuggestionResponseDTO;
import com.example.pharmacy.dto.DoctorPatientSuggestionItemDTO;
import com.example.pharmacy.dto.DoctorPatientSuggestionResponseDTO;
import com.example.pharmacy.entity.MedicineStockItem;
import com.example.pharmacy.entity.PatientPharmacyPreference;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PrescriptionOrder;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PatientPharmacyPreferenceRepository;
import com.example.pharmacy.repository.PrescriptionOrderRepository;
import com.example.pharmacy.service.DoctorLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final PrescriptionOrderRepository prescriptionOrderRepository;

    @Value("${app.user-db.url:jdbc:mysql://localhost:3306/healthcare_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}")
    private String userDbUrl;

    @Value("${app.user-db.username:root}")
    private String userDbUsername;

    @Value("${app.user-db.password:}")
    private String userDbPassword;

    @Override
    public DoctorPatientSuggestionResponseDTO suggestPatients(String query) {
        String normalizedQuery = normalizeQuery(query);
        String queryLower = normalizedQuery.toLowerCase();

        Map<Long, DoctorPatientSuggestionItemDTO> distinctByPatient = new LinkedHashMap<>();

        // Primary source: all patients from user database.
        for (DoctorPatientSuggestionItemDTO candidate : fetchPatientsFromUserDb(queryLower)) {
            if (!distinctByPatient.containsKey(candidate.getPatientId())) {
                distinctByPatient.put(candidate.getPatientId(), candidate);
            }
            if (distinctByPatient.size() >= 20) {
                break;
            }
        }

        // Fallback source: names already seen in pharmacy-service prescription data.
        List<PrescriptionOrder> orders = prescriptionOrderRepository.findAll().stream()
            .sorted(Comparator.comparing(PrescriptionOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .toList();

        for (PrescriptionOrder order : orders) {
            String patientName = order.getPatientName();
            if (patientName == null || patientName.isBlank()) {
                continue;
            }

            if (!patientName.toLowerCase().contains(queryLower)) {
                continue;
            }

            if (distinctByPatient.containsKey(order.getPatientId())) {
                continue;
            }

            distinctByPatient.put(order.getPatientId(), DoctorPatientSuggestionItemDTO.builder()
                .patientId(order.getPatientId())
                .displayName(patientName)
                .build());

            if (distinctByPatient.size() >= 20) {
                break;
            }
        }

        return DoctorPatientSuggestionResponseDTO.builder()
            .suggestions(new ArrayList<>(distinctByPatient.values()))
            .build();
    }

    private List<DoctorPatientSuggestionItemDTO> fetchPatientsFromUserDb(String queryLower) {
        String sql = """
                        SELECT u.id, u.first_name, u.last_name, up.avatar
                        FROM users u
                        LEFT JOIN user_profiles up ON up.user_id = u.id
                        WHERE u.role = 'PATIENT'
                            AND (u.is_active = 1 OR u.is_active IS NULL)
                        ORDER BY u.created_at DESC
            """;

        List<DoctorPatientSuggestionItemDTO> result = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(userDbUrl, userDbUsername, userDbPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String avatar = rs.getString("avatar");
                String displayName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();

                if (displayName.isBlank() || !displayName.toLowerCase().contains(queryLower)) {
                    continue;
                }

                result.add(DoctorPatientSuggestionItemDTO.builder()
                    .patientId(id)
                    .displayName(displayName)
                    .profilePictureUrl(avatar == null || avatar.isBlank() ? null : avatar)
                    .build());

                if (result.size() >= 20) {
                    break;
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }

        return result;
    }

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
