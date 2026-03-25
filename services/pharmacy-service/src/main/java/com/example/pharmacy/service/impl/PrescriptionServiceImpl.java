package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.PrescriptionCreateRequestDTO;
import com.example.pharmacy.dto.PrescriptionAlternativeResponseDTO;
import com.example.pharmacy.dto.AlternativePharmacyOptionDTO;
import com.example.pharmacy.dto.PrescriptionLineCreateRequestDTO;
import com.example.pharmacy.dto.PrescriptionLineResponseDTO;
import com.example.pharmacy.dto.PerMedicineAlternativeDTO;
import com.example.pharmacy.dto.PrescriptionPharmacyReassignRequestDTO;
import com.example.pharmacy.dto.PrescriptionResponseDTO;
import com.example.pharmacy.dto.PrescriptionStatusUpdateRequestDTO;
import com.example.pharmacy.entity.MedicineStockItem;
import com.example.pharmacy.entity.PatientPharmacyPreference;
import com.example.pharmacy.entity.Pharmacy;
import com.example.pharmacy.entity.PrescriptionLine;
import com.example.pharmacy.entity.PrescriptionOrder;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.entity.StockState;
import com.example.pharmacy.exception.ResourceNotFoundException;
import com.example.pharmacy.repository.MedicineStockItemRepository;
import com.example.pharmacy.repository.PatientPharmacyPreferenceRepository;
import com.example.pharmacy.repository.PharmacyRepository;
import com.example.pharmacy.repository.PrescriptionOrderRepository;
import com.example.pharmacy.security.CurrentUserService;
import com.example.pharmacy.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final double FULL_MATCH_RADIUS_KM = 10.0;
    private static final double PARTIAL_FALLBACK_RADIUS_KM = 20.0;

    private static final Map<PrescriptionStatus, List<PrescriptionStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        PrescriptionStatus.PENDING, List.of(PrescriptionStatus.ACCEPTED, PrescriptionStatus.REJECTED),
        PrescriptionStatus.ACCEPTED, List.of(PrescriptionStatus.READY_FOR_PICKUP, PrescriptionStatus.REJECTED),
        PrescriptionStatus.READY_FOR_PICKUP, List.of(PrescriptionStatus.COLLECTED),
        PrescriptionStatus.REJECTED, List.of(),
        PrescriptionStatus.COLLECTED, List.of(),
        PrescriptionStatus.EXPIRED, List.of()
    );

    private final PrescriptionOrderRepository prescriptionOrderRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineStockItemRepository medicineStockItemRepository;
    private final PatientPharmacyPreferenceRepository preferenceRepository;
    private final CurrentUserService currentUserService;

    @Override
    public PrescriptionResponseDTO createPrescription(PrescriptionCreateRequestDTO request) {
        Long doctorId = currentUserService.getCurrentUserId();
        Pharmacy pharmacy = resolvePharmacyForPrescription(request);
        List<PrescriptionLineCreateRequestDTO> lineRequests = resolveLineRequests(request);
        PrescriptionLineCreateRequestDTO summaryLine = lineRequests.get(0);

        PrescriptionOrder order = PrescriptionOrder.builder()
            .pharmacy(pharmacy)
            .doctorId(doctorId)
            .patientId(request.getPatientId())
            .doctorName(request.getDoctorName())
            .patientName(request.getPatientName())
            .medicationName(summaryLine.getMedicationName())
            .dosage(summaryLine.getDosage())
            .quantity(summaryLine.getQuantity())
            .instructions(summaryLine.getInstructions())
            .status(PrescriptionStatus.PENDING)
            .build();

        List<PrescriptionLine> lines = new ArrayList<>();
        for (PrescriptionLineCreateRequestDTO lineRequest : lineRequests) {
            lines.add(PrescriptionLine.builder()
                .prescriptionOrder(order)
                .medicationName(lineRequest.getMedicationName())
                .dosage(lineRequest.getDosage())
                .quantity(lineRequest.getQuantity())
                .instructions(lineRequest.getInstructions())
                .build());
        }
        order.setMedicineLines(lines);

        return toResponse(prescriptionOrderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getMyInbox() {
        Long pharmacistId = currentUserService.getCurrentUserId();
        return prescriptionOrderRepository.findByPharmacyOwnerUserIdOrderByCreatedAtDesc(pharmacistId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getMyPatientPrescriptions() {
        Long patientId = currentUserService.getCurrentUserId();
        return prescriptionOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescription(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        PrescriptionOrder order = prescriptionOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        boolean isOwnerPharmacist = isPharmacyOwner(order, currentUserId);
        boolean isPatient = order.getPatientId().equals(currentUserId);
        boolean isDoctor = order.getDoctorId().equals(currentUserId);

        if (!(isOwnerPharmacist || isPatient || isDoctor)) {
            throw new IllegalStateException("You cannot access this prescription");
        }

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionAlternativeResponseDTO getPatientAlternatives(Long id, Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        Long patientId = currentUserService.getCurrentUserId();
        PrescriptionOrder order = getOwnedPatientPrescription(id, patientId);

        if (order.getStatus() != PrescriptionStatus.PENDING) {
            throw new IllegalStateException("Alternative pharmacy recommendations are only available for pending prescriptions");
        }

        List<RequiredLine> requiredLines = resolveRequiredLines(order);
        if (requiredLines.isEmpty()) {
            throw new IllegalStateException("Prescription does not contain medicine lines");
        }

        List<PharmacyDistance> nearby10 = findNearbyPharmacies(latitude, longitude, FULL_MATCH_RADIUS_KM);
        Set<String> requiredMedicineNames = requiredLines.stream().map(RequiredLine::normalizedName).collect(Collectors.toSet());

        Map<Long, Map<String, Integer>> stockMap10 = loadStockByPharmacy(nearby10, requiredMedicineNames);
        List<AlternativePharmacyOptionDTO> fullMatchPharmacies = nearby10.stream()
            .filter(candidate -> hasAllRequiredLines(stockMap10.getOrDefault(candidate.pharmacy().getId(), Map.of()), requiredLines))
            .map(candidate -> toAlternativeOption(candidate, null))
            .toList();

        if (!fullMatchPharmacies.isEmpty()) {
            return PrescriptionAlternativeResponseDTO.builder()
                .prescriptionId(order.getId())
                .status(order.getStatus())
                .latitude(latitude)
                .longitude(longitude)
                .fullMatchRadiusKm(FULL_MATCH_RADIUS_KM)
                .partialRadiusKm(PARTIAL_FALLBACK_RADIUS_KM)
                .recommendedMode("FULL_MATCH")
                .message("Nearby pharmacies can fulfill all medicine lines")
                .fullMatchPharmacies(fullMatchPharmacies)
                .perMedicineAlternatives(List.of())
                .selectablePharmacies(fullMatchPharmacies)
                .build();
        }

        List<PharmacyDistance> nearby20 = findNearbyPharmacies(latitude, longitude, PARTIAL_FALLBACK_RADIUS_KM);
        Map<Long, Map<String, Integer>> stockMap20 = loadStockByPharmacy(nearby20, requiredMedicineNames);

        List<PerMedicineAlternativeDTO> perMedicineAlternatives = new ArrayList<>();
        Map<Long, AlternativePharmacyOptionDTO> selectable = new LinkedHashMap<>();

        for (RequiredLine requiredLine : requiredLines) {
            List<AlternativePharmacyOptionDTO> options = nearby20.stream()
                .map(candidate -> {
                    Map<String, Integer> stock = stockMap20.getOrDefault(candidate.pharmacy().getId(), Map.of());
                    if (!stock.containsKey(requiredLine.normalizedName())) {
                        return null;
                    }
                    return toAlternativeOption(candidate, stock.get(requiredLine.normalizedName()));
                })
                .filter(option -> option != null)
                .toList();

            options.forEach(option -> selectable.put(option.getPharmacyId(), option));

            perMedicineAlternatives.add(PerMedicineAlternativeDTO.builder()
                .lineId(requiredLine.lineId())
                .medicationName(requiredLine.displayName())
                .requiredQuantity(requiredLine.requiredQuantity())
                .pharmacies(options)
                .build());
        }

        return PrescriptionAlternativeResponseDTO.builder()
            .prescriptionId(order.getId())
            .status(order.getStatus())
            .latitude(latitude)
            .longitude(longitude)
            .fullMatchRadiusKm(FULL_MATCH_RADIUS_KM)
            .partialRadiusKm(PARTIAL_FALLBACK_RADIUS_KM)
            .recommendedMode(selectable.isEmpty() ? "NONE" : "PARTIAL_FALLBACK")
            .message(selectable.isEmpty()
                ? "No nearby pharmacies currently match the prescription requirements"
                : "No full-match pharmacy found within 10 km. Showing per-medicine alternatives within 20 km.")
            .fullMatchPharmacies(List.of())
            .perMedicineAlternatives(perMedicineAlternatives)
            .selectablePharmacies(new ArrayList<>(selectable.values()))
            .build();
    }

    @Override
    public PrescriptionResponseDTO reassignPatientPrescriptionPharmacy(Long id, PrescriptionPharmacyReassignRequestDTO request) {
        validateCoordinates(request.getLatitude(), request.getLongitude());
        Long patientId = currentUserService.getCurrentUserId();
        PrescriptionOrder order = getOwnedPatientPrescription(id, patientId);

        if (order.getStatus() != PrescriptionStatus.PENDING) {
            throw new IllegalStateException("Only pending prescriptions can be reassigned to another pharmacy");
        }

        PrescriptionAlternativeResponseDTO alternatives = getPatientAlternatives(id, request.getLatitude(), request.getLongitude());
        Set<Long> candidatePharmacyIds = alternatives.getSelectablePharmacies().stream()
            .map(AlternativePharmacyOptionDTO::getPharmacyId)
            .collect(Collectors.toSet());

        if (!candidatePharmacyIds.contains(request.getPharmacyId())) {
            throw new IllegalArgumentException("Selected pharmacy is not part of the recommendation results");
        }

        Pharmacy selectedPharmacy = pharmacyRepository.findById(request.getPharmacyId())
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", request.getPharmacyId()));

        order.setPharmacy(selectedPharmacy);
        return toResponse(prescriptionOrderRepository.save(order));
    }

    @Override
    public PrescriptionResponseDTO updatePrescriptionStatus(Long id, PrescriptionStatusUpdateRequestDTO request) {
        Long pharmacistId = currentUserService.getCurrentUserId();
        PrescriptionOrder order = prescriptionOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        ensureAssignedToPharmacyOwner(order, pharmacistId);

        PrescriptionStatus nextStatus = request.getStatus();
        if (nextStatus == PrescriptionStatus.PENDING || nextStatus == PrescriptionStatus.EXPIRED) {
            throw new IllegalArgumentException("Invalid status update for pharmacist workflow");
        }

        if (nextStatus == order.getStatus()) {
            throw new IllegalArgumentException("Prescription is already in status " + order.getStatus());
        }

        if (!isAllowedTransition(order.getStatus(), nextStatus)) {
            throw new IllegalStateException(
                "Invalid status transition from " + order.getStatus() + " to " + nextStatus
            );
        }

        String normalizedRejectionReason = normalizeRejectionReason(request.getRejectionReason());
        if (nextStatus == PrescriptionStatus.REJECTED && normalizedRejectionReason == null) {
            throw new IllegalArgumentException("Rejection reason is required when rejecting a prescription");
        }

        order.setStatus(nextStatus);
        order.setRejectionReason(nextStatus == PrescriptionStatus.REJECTED ? normalizedRejectionReason : null);

        if (nextStatus == PrescriptionStatus.READY_FOR_PICKUP) {
            order.setReadyAt(LocalDateTime.now());
        } else if (nextStatus == PrescriptionStatus.ACCEPTED || nextStatus == PrescriptionStatus.REJECTED) {
            order.setReadyAt(null);
        }

        return toResponse(prescriptionOrderRepository.save(order));
    }

    private PrescriptionResponseDTO toResponse(PrescriptionOrder order) {
        Pharmacy pharmacy = order.getPharmacy();
        List<PrescriptionLineResponseDTO> lines = order.getMedicineLines() == null
            ? List.of()
            : order.getMedicineLines().stream().map(this::toLineResponse).toList();

        PrescriptionLineResponseDTO summaryLine = lines.isEmpty() ? null : lines.get(0);

        return PrescriptionResponseDTO.builder()
            .id(order.getId())
            .pharmacyId(pharmacy != null ? pharmacy.getId() : null)
            .pharmacyName(pharmacy != null ? pharmacy.getName() : null)
            .doctorId(order.getDoctorId())
            .patientId(order.getPatientId())
            .doctorName(order.getDoctorName())
            .patientName(order.getPatientName())
            .assignedToPharmacy(pharmacy != null)
            .assignmentMessage(pharmacy == null ? "Patient has no default pharmacy yet" : null)
            .medicationName(summaryLine != null ? summaryLine.getMedicationName() : order.getMedicationName())
            .dosage(summaryLine != null ? summaryLine.getDosage() : order.getDosage())
            .quantity(summaryLine != null ? summaryLine.getQuantity() : order.getQuantity())
            .instructions(summaryLine != null ? summaryLine.getInstructions() : order.getInstructions())
            .medicineLines(lines)
            .status(order.getStatus())
            .rejectionReason(order.getRejectionReason())
            .readyAt(order.getReadyAt() != null ? order.getReadyAt().toString() : null)
            .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
            .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)
            .build();
    }

    private PrescriptionLineResponseDTO toLineResponse(PrescriptionLine line) {
        return PrescriptionLineResponseDTO.builder()
            .id(line.getId())
            .medicationName(line.getMedicationName())
            .dosage(line.getDosage())
            .quantity(line.getQuantity())
            .instructions(line.getInstructions())
            .build();
    }

    private Pharmacy resolvePharmacyForPrescription(PrescriptionCreateRequestDTO request) {
        if (request.getPharmacyId() != null) {
            return pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", "id", request.getPharmacyId()));
        }

        return preferenceRepository.findByPatientId(request.getPatientId())
            .map(PatientPharmacyPreference::getDefaultPharmacy)
            .orElse(null);
    }

    private List<PrescriptionLineCreateRequestDTO> resolveLineRequests(PrescriptionCreateRequestDTO request) {
        List<PrescriptionLineCreateRequestDTO> explicitLines = request.getMedicineLines();
        if (explicitLines == null || explicitLines.isEmpty()) {
            throw new IllegalArgumentException("At least one medicine line is required");
        }

        return explicitLines.stream().map(this::validateAndNormalizeLine).toList();
    }

    private PrescriptionLineCreateRequestDTO validateAndNormalizeLine(PrescriptionLineCreateRequestDTO line) {
        if (line == null) {
            throw new IllegalArgumentException("At least one medicine line is required");
        }

        String medicationName = line.getMedicationName() == null ? null : line.getMedicationName().trim();
        String dosage = line.getDosage() == null ? null : line.getDosage().trim();

        if (medicationName == null || medicationName.isEmpty()) {
            throw new IllegalArgumentException("Medication name is required for every medicine line");
        }

        if (dosage == null || dosage.isEmpty()) {
            throw new IllegalArgumentException("Dosage is required for every medicine line");
        }

        if (line.getQuantity() == null || line.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero for every medicine line");
        }

        String instructions = line.getInstructions() == null ? null : line.getInstructions().trim();
        if (instructions != null && instructions.isEmpty()) {
            instructions = null;
        }

        return PrescriptionLineCreateRequestDTO.builder()
            .medicationName(medicationName)
            .dosage(dosage)
            .quantity(line.getQuantity())
            .instructions(instructions)
            .build();
    }

    private boolean isAllowedTransition(PrescriptionStatus currentStatus, PrescriptionStatus nextStatus) {
        return ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, List.of()).contains(nextStatus);
    }

    private String normalizeRejectionReason(String rejectionReason) {
        if (rejectionReason == null) {
            return null;
        }
        String normalized = rejectionReason.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void ensureAssignedToPharmacyOwner(PrescriptionOrder order, Long pharmacistId) {
        if (order.getPharmacy() == null) {
            throw new IllegalStateException("This prescription is not yet assigned to a pharmacy");
        }

        if (!isPharmacyOwner(order, pharmacistId)) {
            throw new IllegalStateException("You can only update prescriptions assigned to your pharmacy");
        }
    }

    private boolean isPharmacyOwner(PrescriptionOrder order, Long userId) {
        return order.getPharmacy() != null && order.getPharmacy().getOwnerUserId().equals(userId);
    }

    private PrescriptionOrder getOwnedPatientPrescription(Long prescriptionId, Long patientId) {
        PrescriptionOrder order = prescriptionOrderRepository.findById(prescriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", prescriptionId));

        if (!order.getPatientId().equals(patientId)) {
            throw new IllegalStateException("You can only manage your own prescriptions");
        }

        return order;
    }

    private List<RequiredLine> resolveRequiredLines(PrescriptionOrder order) {
        if (order.getMedicineLines() != null && !order.getMedicineLines().isEmpty()) {
            List<RequiredLine> lines = new ArrayList<>();
            for (PrescriptionLine line : order.getMedicineLines()) {
                String displayName = safeTrim(line.getMedicationName());
                String normalizedName = normalizeMedicineName(displayName);
                if (normalizedName == null) {
                    continue;
                }

                int requiredQuantity = line.getQuantity() == null || line.getQuantity() <= 0 ? 1 : line.getQuantity();
                lines.add(new RequiredLine(line.getId(), displayName, normalizedName, requiredQuantity));
            }
            return lines;
        }

        String displayName = safeTrim(order.getMedicationName());
        String normalizedName = normalizeMedicineName(displayName);
        if (normalizedName == null) {
            return List.of();
        }

        int requiredQuantity = order.getQuantity() == null || order.getQuantity() <= 0 ? 1 : order.getQuantity();
        return List.of(new RequiredLine(order.getId(), displayName, normalizedName, requiredQuantity));
    }

    private List<PharmacyDistance> findNearbyPharmacies(Double latitude, Double longitude, double maxRadiusKm) {
        return pharmacyRepository.findAllWithCoordinates().stream()
            .map(pharmacy -> new PharmacyDistance(
                pharmacy,
                roundDistance(calculateDistanceKm(latitude, longitude, pharmacy.getLatitude(), pharmacy.getLongitude()))
            ))
            .filter(candidate -> candidate.distanceKm() <= maxRadiusKm)
            .sorted(Comparator.comparingDouble(PharmacyDistance::distanceKm))
            .limit(30)
            .toList();
    }

    private Map<Long, Map<String, Integer>> loadStockByPharmacy(
        List<PharmacyDistance> candidates,
        Set<String> medicineNames
    ) {
        if (candidates.isEmpty() || medicineNames.isEmpty()) {
            return Map.of();
        }

        Set<Long> pharmacyIds = candidates.stream().map(candidate -> candidate.pharmacy().getId()).collect(Collectors.toSet());
        List<MedicineStockItem> stockItems = medicineStockItemRepository.findActiveByPharmacyIdsAndMedicineNames(pharmacyIds, medicineNames);

        Map<Long, Map<String, Integer>> stockByPharmacy = new LinkedHashMap<>();
        for (MedicineStockItem stockItem : stockItems) {
            if (stockItem.getState() != null && stockItem.getState() != StockState.IN_STOCK) {
                continue;
            }
            Long pharmacyId = stockItem.getPharmacy().getId();
            String normalizedName = normalizeMedicineName(stockItem.getMedicineName());
            if (normalizedName == null) {
                continue;
            }

            stockByPharmacy.putIfAbsent(pharmacyId, new LinkedHashMap<>());
            Map<String, Integer> medicineStock = stockByPharmacy.get(pharmacyId);
            int safeQuantity = stockItem.getQuantity() == null ? 0 : Math.max(stockItem.getQuantity(), 0);
            medicineStock.put(normalizedName, medicineStock.getOrDefault(normalizedName, 0) + safeQuantity);
        }

        return stockByPharmacy;
    }

    private boolean hasAllRequiredLines(Map<String, Integer> pharmacyStock, List<RequiredLine> requiredLines) {
        for (RequiredLine requiredLine : requiredLines) {
            if (!pharmacyStock.containsKey(requiredLine.normalizedName())) {
                return false;
            }
        }
        return true;
    }

    private AlternativePharmacyOptionDTO toAlternativeOption(PharmacyDistance candidate, Integer availableQuantity) {
        Pharmacy pharmacy = candidate.pharmacy();
        return AlternativePharmacyOptionDTO.builder()
            .pharmacyId(pharmacy.getId())
            .pharmacyName(pharmacy.getName())
            .addressLine(pharmacy.getAddressLine())
            .city(pharmacy.getCity())
            .governorate(pharmacy.getGovernorate())
            .latitude(pharmacy.getLatitude())
            .longitude(pharmacy.getLongitude())
            .distanceKm(candidate.distanceKm())
            .availableQuantity(availableQuantity)
            .build();
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private double roundDistance(double distance) {
        return Math.round(distance * 100.0) / 100.0;
    }

    private String normalizeMedicineName(String medicineName) {
        String value = safeTrim(medicineName);
        return value == null ? null : value.toLowerCase();
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record RequiredLine(Long lineId, String displayName, String normalizedName, Integer requiredQuantity) {}

    private record PharmacyDistance(Pharmacy pharmacy, Double distanceKm) {}
}
