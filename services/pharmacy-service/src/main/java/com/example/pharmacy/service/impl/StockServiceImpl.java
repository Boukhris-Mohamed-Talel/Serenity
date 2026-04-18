package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.StockItemCreateRequestDTO;
import com.example.pharmacy.dto.StockItemResponseDTO;
import com.example.pharmacy.dto.StockReorderSuggestionDTO;
import com.example.pharmacy.entity.MedicineStockItem;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.StockForecastPrediction;
import com.example.pharmacy.entity.StockState;
import com.example.pharmacy.entity.StockoutRisk;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.repository.StockForecastPredictionRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockServiceImpl implements StockService {

    private final MedicineStockItemRepository medicineStockItemRepository;
    private final PharmacyRepository pharmacyRepository;
    private final StockForecastPredictionRepository stockForecastPredictionRepository;
    private final StockForecastAiClient stockForecastAiClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<StockItemResponseDTO> listMyStock(String query, boolean includeArchived) {
        Long userId = currentUserService.getCurrentUserId();
        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();
        List<MedicineStockItem> items = medicineStockItemRepository.findByOwnerUserIdWithSearch(
            userId,
            includeArchived,
            normalizedQuery
        );

        return items.stream().map(this::toResponse).toList();
    }

    @Override
    public StockItemResponseDTO createStockItem(StockItemCreateRequestDTO request) {
        Long userId = currentUserService.getCurrentUserId();
        Pharmacy pharmacy = pharmacyRepository.findByOwnerUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "ownerUserId", userId));

        MedicineStockItem item = MedicineStockItem.builder()
            .pharmacy(pharmacy)
            .medicineName(request.getMedicineName().trim())
            .quantity(request.getQuantity())
            .imageUrl(request.getImageUrl())
            .description(request.getDescription())
            .state(request.getQuantity() > 0 ? StockState.IN_STOCK : StockState.OUT_OF_STOCK)
            .archived(false)
            .build();

        return toResponse(medicineStockItemRepository.save(item));
    }

    @Override
    public StockItemResponseDTO renameStockItem(Long stockItemId, String medicineName) {
        MedicineStockItem item = getOwnedItem(stockItemId);
        String normalizedName = medicineName == null ? "" : medicineName.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Medicine name is required");
        }
        if (normalizedName.length() < 2 || normalizedName.length() > 80) {
            throw new IllegalArgumentException("Medicine name must be between 2 and 80 characters");
        }

        item.setMedicineName(normalizedName);
        return toResponse(medicineStockItemRepository.save(item));
    }

    @Override
    public StockItemResponseDTO incrementQuantity(Long stockItemId, Integer incrementBy) {
        MedicineStockItem item = getOwnedItem(stockItemId);
        item.setQuantity(item.getQuantity() + incrementBy);
        item.setState(item.getQuantity() > 0 ? StockState.IN_STOCK : StockState.OUT_OF_STOCK);
        item.setArchived(false);
        return toResponse(medicineStockItemRepository.save(item));
    }

    @Override
    public StockItemResponseDTO markOutOfStock(Long stockItemId) {
        MedicineStockItem item = getOwnedItem(stockItemId);
        item.setQuantity(0);
        item.setState(StockState.OUT_OF_STOCK);
        return toResponse(medicineStockItemRepository.save(item));
    }

    @Override
    public StockItemResponseDTO restoreStockItem(Long stockItemId) {
        MedicineStockItem item = getOwnedItem(stockItemId);
        item.setArchived(false);
        int safeQuantity = item.getQuantity() == null ? 0 : item.getQuantity();
        item.setState(safeQuantity > 0 ? StockState.IN_STOCK : StockState.OUT_OF_STOCK);
        return toResponse(medicineStockItemRepository.save(item));
    }

    @Override
    public void archiveStockItem(Long stockItemId) {
        MedicineStockItem item = getOwnedItem(stockItemId);
        item.setArchived(true);
        medicineStockItemRepository.save(item);
    }

    @Override
    public void deleteArchivedStockItem(Long stockItemId) {
        MedicineStockItem item = getOwnedItem(stockItemId);
        if (!Boolean.TRUE.equals(item.getArchived())) {
            throw new IllegalStateException("Only archived medicines can be deleted permanently");
        }
        medicineStockItemRepository.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockReorderSuggestionDTO> getMyReorderSuggestions(int limit) {
        Pharmacy pharmacy = getMyPharmacy();
        List<StockForecastPrediction> predictionRows = stockForecastPredictionRepository
            .findLatestRunRowsByPharmacyId(pharmacy.getId());
        if (predictionRows.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> currentQtyByMedicine = medicineStockItemRepository
            .sumActiveQuantitiesByPharmacyId(pharmacy.getId())
            .stream()
            .collect(Collectors.toMap(
                row -> row.getMedicineName() == null ? "" : row.getMedicineName(),
                row -> row.getTotalQuantity() == null ? 0 : row.getTotalQuantity().intValue()
            ));

        return predictionRows.stream()
            .collect(Collectors.groupingBy(
                row -> normalizeMedicineName(row.getMedicineName()),
                Collectors.toList()
            ))
            .values()
            .stream()
            .map(rows -> toSuggestion(rows, currentQtyByMedicine))
            .sorted(Comparator
                .comparingInt((StockReorderSuggestionDTO dto) -> riskRank(dto.getStockoutRisk()))
                .reversed()
                .thenComparing(StockReorderSuggestionDTO::getSuggestedReorderQty, Comparator.reverseOrder())
                .thenComparing(StockReorderSuggestionDTO::getDemand14, Comparator.reverseOrder())
            )
            .limit(limit)
            .toList();
    }

    @Override
    public void refreshMyReorderSuggestions() {
        Pharmacy pharmacy = getMyPharmacy();
        stockForecastAiClient.runForecast(pharmacy.getId());
    }

    private MedicineStockItem getOwnedItem(Long stockItemId) {
        Long userId = currentUserService.getCurrentUserId();
        return medicineStockItemRepository.findByIdAndPharmacyOwnerUserId(stockItemId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("StockItem", "id", stockItemId));
    }

    private Pharmacy getMyPharmacy() {
        Long userId = currentUserService.getCurrentUserId();
        return pharmacyRepository.findByOwnerUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "ownerUserId", userId));
    }

    private StockReorderSuggestionDTO toSuggestion(
        List<StockForecastPrediction> rows,
        Map<String, Integer> currentQtyByMedicine
    ) {
        StockForecastPrediction first = rows.get(0);
        String normalizedName = normalizeMedicineName(first.getMedicineName());
        int currentQty = currentQtyByMedicine.getOrDefault(normalizedName, 0);
        double demand14 = rows.stream()
            .map(StockForecastPrediction::getPredictedDemand)
            .filter(value -> value != null)
            .mapToDouble(Double::doubleValue)
            .sum();
        int suggestedReorderQty = rows.stream()
            .map(StockForecastPrediction::getSuggestedReorderQty)
            .filter(value -> value != null)
            .max(Integer::compareTo)
            .orElse(0);
        StockoutRisk highestRisk = rows.stream()
            .map(StockForecastPrediction::getStockoutRisk)
            .max(Comparator.comparingInt(this::riskRank))
            .orElse(StockoutRisk.LOW);

        return StockReorderSuggestionDTO.builder()
            .medicineName(first.getMedicineName())
            .currentQty(currentQty)
            .demand14(demand14)
            .suggestedReorderQty(suggestedReorderQty)
            .stockoutRisk(highestRisk)
            .generatedAt(first.getRunAt() != null ? first.getRunAt().toString() : null)
            .build();
    }

    private int riskRank(StockoutRisk risk) {
        if (risk == null) {
            return 0;
        }
        return switch (risk) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private String normalizeMedicineName(String medicineName) {
        if (medicineName == null) {
            return "";
        }
        return medicineName.trim().toLowerCase(Locale.ROOT);
    }

    private StockItemResponseDTO toResponse(MedicineStockItem item) {
        return StockItemResponseDTO.builder()
            .id(item.getId())
            .pharmacyId(item.getPharmacy().getId())
            .medicineName(item.getMedicineName())
            .quantity(item.getQuantity())
            .imageUrl(item.getImageUrl())
            .description(item.getDescription())
            .state(item.getState())
            .archived(item.getArchived())
            .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null)
            .updatedAt(item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : null)
            .build();
    }
}
