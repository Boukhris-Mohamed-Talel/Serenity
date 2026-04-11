package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.dto.MoodEntryResponseDTO;
import com.serenity.monitoring.dto.CrisisAlertPayload;
import com.serenity.monitoring.dto.DoctorMonitoringDashboardDTO;
import com.serenity.monitoring.dto.MoodTrendPointDTO;
import com.serenity.monitoring.dto.PatientMoodPointDTO;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.mapper.MoodEntryMapper;
import com.serenity.monitoring.entity.UserProfileSnapshot;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.repository.UserProfileSnapshotRepository;
import com.serenity.monitoring.service.DoctorAssignmentService;
import com.serenity.monitoring.service.CrisisAlertService;
import com.serenity.monitoring.service.MoodEntryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Date;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MoodEntryServiceImpl implements MoodEntryService {

    private final MoodEntryRepository moodEntryRepository;
    private final MoodEntryMapper moodEntryMapper;
    private final DoctorAssignmentService doctorAssignmentService;
    private final CrisisAlertService crisisAlertService;
    private final EmotionalTriggerRepository emotionalTriggerRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileSnapshotRepository userProfileSnapshotRepository;

    @Override
    public MoodEntryResponseDTO createMoodEntry(MoodEntryRequestDTO request) {
        // Get or assign doctor for this patient (Round-Robin algorithm)
        Long assignedDoctorId = doctorAssignmentService.getOrAssignDoctor(request.getPatientId());
        log.info("Creating mood entry for patientId={} assignedDoctorId={} moodScore={}",
                request.getPatientId(), assignedDoctorId, request.getMoodScore());
        
        // Set the doctor ID in the request
        request.setDoctorId(assignedDoctorId);
        
        MoodEntry moodEntry = moodEntryMapper.toEntity(request);
        MoodEntry savedEntry = moodEntryRepository.save(moodEntry);

        if (savedEntry.getMoodScore() != null && savedEntry.getMoodScore() <= 3) {
            UserAccount patient = userAccountRepository.findById(savedEntry.getPatientId()).orElse(null);
            String patientName = patient != null ? buildDisplayName(patient) : "Unknown Patient";

            CrisisAlertPayload payload = CrisisAlertPayload.builder()
                    .doctorId(savedEntry.getDoctorId())
                    .patientId(savedEntry.getPatientId())
                    .patientFullName(patientName)
                    .moodLevel(savedEntry.getMoodScore())
                    .message("Crisis alert: " + patientName + " submitted a low mood score")
                    .timestamp(savedEntry.getCreatedAt() != null ? savedEntry.getCreatedAt() : new Date())
                    .build();

            crisisAlertService.sendCrisisAlert(payload);
        } else {
            log.debug("Mood entry {} not considered crisis (moodScore={})",
                    savedEntry.getId(), savedEntry.getMoodScore());
        }

        return enrichResponse(moodEntryMapper.toResponseDTO(savedEntry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoodEntryResponseDTO> getMoodEntriesByPatient(Long patientId) {
        List<MoodEntry> entries = moodEntryRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return enrichResponseList(moodEntryMapper.toResponseDTOList(entries));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoodEntryResponseDTO> getMoodEntriesByDoctor(Long doctorId) {
        List<MoodEntry> entries = moodEntryRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        return enrichResponseList(moodEntryMapper.toResponseDTOList(entries));
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorMonitoringDashboardDTO getDoctorDashboard(Long doctorId) {
        List<MoodEntry> entries = moodEntryRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        if (entries.isEmpty()) {
            return DoctorMonitoringDashboardDTO.builder()
                    .totalPatients(0L)
                    .totalMoodEntries(0L)
                    .totalClinicalTriggers(0L)
                    .crisisEvents(0L)
                    .averageMood(0.0)
                    .averageMoodChange(0.0)
                    .activeHighRiskPatients(0L)
                    .moodTrend(Collections.emptyList())
                    .patientPoints(Collections.emptyList())
                    .build();
        }

        Set<Long> patientIds = entries.stream()
                .map(MoodEntry::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserAccount> patientsById = new HashMap<>();
        userAccountRepository.findAllById(patientIds)
                .forEach(user -> patientsById.put(user.getId(), user));

        Map<Long, UserProfileSnapshot> profileByPatientId = new HashMap<>();
        if (!patientIds.isEmpty()) {
            userProfileSnapshotRepository.findAllByUserIdIn(patientIds)
                    .forEach(profile -> profileByPatientId.put(profile.getUserId(), profile));
        }

        List<Long> moodEntryIds = entries.stream()
                .map(MoodEntry::getId)
                .filter(Objects::nonNull)
                .toList();

        List<EmotionalTrigger> allTriggers = moodEntryIds.isEmpty()
                ? Collections.emptyList()
                : emotionalTriggerRepository.findByMoodEntryIdIn(moodEntryIds);

        Map<Long, List<EmotionalTrigger>> triggersByMoodEntryId = new HashMap<>();
        for (EmotionalTrigger trigger : allTriggers) {
            Long moodEntryId = trigger.getMoodEntry() != null ? trigger.getMoodEntry().getId() : null;
            if (moodEntryId != null) {
                triggersByMoodEntryId.computeIfAbsent(moodEntryId, key -> new ArrayList<>()).add(trigger);
            }
        }

        Map<Long, List<MoodEntry>> entriesByPatient = entries.stream()
                .collect(Collectors.groupingBy(MoodEntry::getPatientId));

        long crisisEvents = entries.stream()
                .filter(entry -> entry.getMoodScore() != null && entry.getMoodScore() <= 3)
                .count();

        double totalMood = entries.stream()
                .filter(entry -> entry.getMoodScore() != null)
                .mapToInt(MoodEntry::getMoodScore)
                .sum();

        List<PatientMoodPointDTO> patientPoints = new ArrayList<>();
        long activeHighRiskPatients = 0;
        double totalMoodChange = 0;

        for (Map.Entry<Long, List<MoodEntry>> patientEntry : entriesByPatient.entrySet()) {
            Long patientId = patientEntry.getKey();
            List<MoodEntry> patientEntries = new ArrayList<>(patientEntry.getValue());
            patientEntries.sort(Comparator.comparing(MoodEntry::getCreatedAt,
                    Comparator.nullsLast(Date::compareTo)));

            MoodEntry firstEntry = patientEntries.get(0);
            MoodEntry latestEntry = patientEntries.get(patientEntries.size() - 1);

            double avgMood = patientEntries.stream()
                    .filter(entry -> entry.getMoodScore() != null)
                    .mapToInt(MoodEntry::getMoodScore)
                    .average()
                    .orElse(0.0);

            long patientCrisisCount = patientEntries.stream()
                    .filter(entry -> entry.getMoodScore() != null && entry.getMoodScore() <= 3)
                    .count();

            int firstMood = firstEntry.getMoodScore() != null ? firstEntry.getMoodScore() : 0;
            int latestMood = latestEntry.getMoodScore() != null ? latestEntry.getMoodScore() : 0;
            double moodChange = latestMood - firstMood;
            totalMoodChange += moodChange;

            if (latestMood <= 3) {
                activeHighRiskPatients++;
            }

            List<EmotionalTrigger> patientTriggers = patientEntries.stream()
                    .map(MoodEntry::getId)
                    .filter(Objects::nonNull)
                    .flatMap(id -> triggersByMoodEntryId.getOrDefault(id, Collections.emptyList()).stream())
                    .toList();

            EmotionalTrigger latestTrigger = patientTriggers.stream()
                    .max(Comparator.comparing(EmotionalTrigger::getRecordedAt,
                            Comparator.nullsLast(LocalDateTime::compareTo)))
                    .orElse(null);

            UserAccount patient = patientsById.get(patientId);
            UserProfileSnapshot profile = profileByPatientId.get(patientId);
            String avatarUrl = profile != null && profile.getAvatar() != null && !profile.getAvatar().isBlank()
                    ? profile.getAvatar().trim()
                    : null;

            PatientMoodPointDTO point = PatientMoodPointDTO.builder()
                    .patientId(patientId)
                    .patientName(patient != null ? buildDisplayName(patient) : "Unknown Patient")
                    .patientAvatarUrl(avatarUrl)
                    .x(0)
                    .latestMoodScore(latestMood)
                    .averageMoodScore(round2(avgMood))
                    .moodChange(round2(moodChange))
                    .entryCount((long) patientEntries.size())
                    .crisisCount(patientCrisisCount)
                    .latestEntryAt(latestEntry.getCreatedAt())
                    .latestTriggerType(latestTrigger != null ? latestTrigger.getTriggerType() : null)
                    .latestTriggerDescription(latestTrigger != null ? latestTrigger.getDescription() : null)
                    .latestTriggerIntensity(latestTrigger != null ? latestTrigger.getIntensity() : null)
                    .latestTriggerAt(latestTrigger != null ? latestTrigger.getRecordedAt() : null)
                    .build();

            patientPoints.add(point);
        }

        patientPoints.sort(Comparator.comparing(PatientMoodPointDTO::getPatientName,
                String.CASE_INSENSITIVE_ORDER));
        for (int i = 0; i < patientPoints.size(); i++) {
            patientPoints.get(i).setX(i + 1);
        }

        double averageMood = entries.isEmpty() ? 0.0 : totalMood / entries.size();
        double averageMoodChange = entriesByPatient.isEmpty() ? 0.0 : totalMoodChange / entriesByPatient.size();

        return DoctorMonitoringDashboardDTO.builder()
                .totalPatients((long) entriesByPatient.size())
                .totalMoodEntries((long) entries.size())
                .totalClinicalTriggers((long) allTriggers.size())
                .crisisEvents(crisisEvents)
                .averageMood(round2(averageMood))
                .averageMoodChange(round2(averageMoodChange))
                .activeHighRiskPatients(activeHighRiskPatients)
                .moodTrend(buildMoodTrend(entries))
                .patientPoints(patientPoints)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MoodEntryResponseDTO getMoodEntryById(Long id) {
        MoodEntry entry = moodEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mood entry not found with ID: " + id));
        return enrichResponse(moodEntryMapper.toResponseDTO(entry));
    }

    @Override
    public MoodEntryResponseDTO updateMoodEntry(Long id, MoodEntryRequestDTO request) {
        MoodEntry entry = moodEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mood entry not found with ID: " + id));

        // Enforce doctor continuity and ownership continuity for this entry.
        // Client payload must never reassign doctor/patient for an existing mood entry.
        Long originalPatientId = entry.getPatientId();
        Long originalDoctorId = entry.getDoctorId();

        moodEntryMapper.updateEntityFromDTO(request, entry);
        entry.setPatientId(originalPatientId);
        entry.setDoctorId(originalDoctorId);

        MoodEntry updatedEntry = moodEntryRepository.save(entry);
        return enrichResponse(moodEntryMapper.toResponseDTO(updatedEntry));
    }

    @Override
    public void deleteMoodEntry(Long id) {
        if (!moodEntryRepository.existsById(id)) {
            throw new IllegalArgumentException("Mood entry not found with ID: " + id);
        }

        if (emotionalTriggerRepository.existsByMoodEntryId(id)) {
            throw new IllegalStateException("Cannot delete this mood entry because it has linked clinical records.");
        }

        moodEntryRepository.deleteById(id);
    }

    private List<MoodEntryResponseDTO> enrichResponseList(List<MoodEntryResponseDTO> responses) {
        if (responses == null || responses.isEmpty()) {
            return responses;
        }

        Set<Long> userIds = new HashSet<>();
        for (MoodEntryResponseDTO response : responses) {
            if (response.getPatientId() != null) {
                userIds.add(response.getPatientId());
            }
            if (response.getDoctorId() != null) {
                userIds.add(response.getDoctorId());
            }
        }

        Map<Long, UserAccount> usersById = new HashMap<>();
        userAccountRepository.findAllById(userIds).forEach(user -> usersById.put(user.getId(), user));

        Set<Long> patientIds = responses.stream()
                .map(MoodEntryResponseDTO::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserProfileSnapshot> profileByPatientId = new HashMap<>();
        if (!patientIds.isEmpty()) {
            userProfileSnapshotRepository.findAllByUserIdIn(patientIds)
                    .forEach(profile -> profileByPatientId.put(profile.getUserId(), profile));
        }

        for (MoodEntryResponseDTO response : responses) {
            UserAccount patient = usersById.get(response.getPatientId());
            if (patient != null) {
                response.setPatientName(buildDisplayName(patient));
            }
            UserProfileSnapshot profile = profileByPatientId.get(response.getPatientId());
            if (profile != null && profile.getAvatar() != null && !profile.getAvatar().isBlank()) {
                response.setPatientAvatarUrl(profile.getAvatar().trim());
            }
            UserAccount doctor = usersById.get(response.getDoctorId());
            if (doctor != null) {
                response.setDoctorName(buildDisplayName(doctor));
            }
        }

        return responses;
    }

    private MoodEntryResponseDTO enrichResponse(MoodEntryResponseDTO response) {
        if (response == null) {
            return null;
        }
        return enrichResponseList(List.of(response)).get(0);
    }

    private String buildDisplayName(UserAccount user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }

    private List<MoodTrendPointDTO> buildMoodTrend(List<MoodEntry> entries) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<MoodEntry> ordered = entries.stream()
                .filter(entry -> entry.getCreatedAt() != null && entry.getMoodScore() != null)
                .sorted(Comparator.comparing(MoodEntry::getCreatedAt))
                .toList();

        if (ordered.isEmpty()) {
            return Collections.emptyList();
        }

        int rollingWindow = Math.min(5, ordered.size());
        long cumulativeCrisis = 0;
        List<MoodTrendPointDTO> trend = new ArrayList<>();

        for (int i = 0; i < ordered.size(); i++) {
            MoodEntry current = ordered.get(i);
            int start = Math.max(0, i - rollingWindow + 1);
            double rollingAverage = ordered.subList(start, i + 1).stream()
                    .mapToInt(MoodEntry::getMoodScore)
                    .average()
                    .orElse(0.0);

            if (current.getMoodScore() <= 3) {
                cumulativeCrisis++;
            }

            trend.add(MoodTrendPointDTO.builder()
                    .date(formatter.format(current.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()))
                    .averageMood(round2(Math.max(1.0, Math.min(10.0, rollingAverage))))
                    .entryCount((long) (i + 1))
                    .crisisCount(cumulativeCrisis)
                    .build());
        }

        return trend;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
