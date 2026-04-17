package com.serenity.monitoring.service;

import com.serenity.monitoring.dto.MonitoringAiCrisisRequest;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.MoodEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the 7-day rolling feature vector expected by monitoring-ai /predict/crisis.
 */
@Service
@RequiredArgsConstructor
public class MoodRiskFeatureService {

    private final MoodEntryRepository moodEntryRepository;
    private final EmotionalTriggerRepository emotionalTriggerRepository;

    public MonitoringAiCrisisRequest buildRequest(MoodEntry latestEntry) {
        Long patientId = latestEntry.getPatientId();
        Date end = new Date();
        Date start = new Date(end.getTime() - 7L * 24 * 60 * 60 * 1000);

        List<MoodEntry> entries = moodEntryRepository.findByPatientIdAndDateRange(patientId, start, end);
        entries.sort(Comparator.comparing(MoodEntry::getCreatedAt, Comparator.nullsLast(Date::compareTo)));

        if (entries.isEmpty()) {
            entries = new ArrayList<>(List.of(latestEntry));
        }

        int totalEntries = entries.size();
        double avgMood = entries.stream()
                .mapToInt(MoodEntry::getMoodScore)
                .average()
                .orElse(latestEntry.getMoodScore());

        long crisisCount = entries.stream()
                .filter(e -> e.getMoodScore() != null && e.getMoodScore() <= 3)
                .count();

        int minMood = entries.stream()
                .mapToInt(MoodEntry::getMoodScore)
                .min()
                .orElse(latestEntry.getMoodScore());

        int daysOfSilence = countSilentDaysInLast7CalendarDays(entries);

        List<Long> moodEntryIds = entries.stream()
                .map(MoodEntry::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        List<EmotionalTrigger> triggers = moodEntryIds.isEmpty()
                ? List.of()
                : emotionalTriggerRepository.findByMoodEntryIdIn(moodEntryIds);

        double triggerIntensityAvg = triggers.isEmpty()
                ? 0.0
                : triggers.stream().mapToInt(EmotionalTrigger::getIntensity).average().orElse(0.0);
        int triggerCount = triggers.size();

        int moodTrend = 0;
        if (entries.size() >= 2) {
            int first = entries.get(0).getMoodScore();
            int last = entries.get(entries.size() - 1).getMoodScore();
            if (last < first) {
                moodTrend = -1;
            } else if (last > first) {
                moodTrend = 1;
            }
        }

        String moodDescriptionText = buildMoodTextContext(entries, latestEntry);
        String triggerText = buildTriggerTextContext(entries, triggers);

        return new MonitoringAiCrisisRequest(
                patientId,
                round2(avgMood),
                (int) crisisCount,
                daysOfSilence,
                round2(triggerIntensityAvg),
                moodTrend,
                minMood,
                triggerCount,
                totalEntries,
                moodDescriptionText,
                triggerText
        );
    }

    private String buildMoodTextContext(List<MoodEntry> entries, MoodEntry latestEntry) {
        String joined = entries.stream()
                .map(MoodEntry::getMoodDescription)
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" || "));

        if (joined.isBlank() && latestEntry.getMoodDescription() != null) {
            joined = latestEntry.getMoodDescription().trim();
        }

        // Keep payload bounded while preserving recent text signal for AI triage.
        int maxLen = 1500;
        if (joined.length() > maxLen) {
            return joined.substring(0, maxLen);
        }
        return joined;
    }

    private String buildTriggerTextContext(List<MoodEntry> entries, List<EmotionalTrigger> triggers) {
        String moodTriggerText = entries.stream()
                .map(MoodEntry::getTriggers)
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" || "));

        String clinicalTriggerText = triggers.stream()
                .map(t -> {
                    String type = t.getTriggerType() != null ? t.getTriggerType().trim() : "";
                    String desc = t.getDescription() != null ? t.getDescription().trim() : "";
                    if (type.isBlank()) {
                        return desc;
                    }
                    if (desc.isBlank()) {
                        return type;
                    }
                    return type + ": " + desc;
                })
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(" || "));

        String joined;
        if (moodTriggerText.isBlank()) {
            joined = clinicalTriggerText;
        } else if (clinicalTriggerText.isBlank()) {
            joined = moodTriggerText;
        } else {
            joined = moodTriggerText + " || " + clinicalTriggerText;
        }

        int maxLen = 1200;
        if (joined.length() > maxLen) {
            return joined.substring(0, maxLen);
        }
        return joined;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Among the last 7 calendar days (today and the 6 previous days), count days with no mood entry.
     */
    private int countSilentDaysInLast7CalendarDays(List<MoodEntry> entries) {
        ZoneId z = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(z);
        LocalDate first = today.minusDays(6);

        Set<LocalDate> daysWithEntry = new HashSet<>();
        for (MoodEntry e : entries) {
            if (e.getCreatedAt() == null) {
                continue;
            }
            LocalDate d = e.getCreatedAt().toInstant().atZone(z).toLocalDate();
            daysWithEntry.add(d);
        }

        int silent = 0;
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            if (!daysWithEntry.contains(d)) {
                silent++;
            }
        }
        return silent;
    }
}
