package com.serenity.monitoring.service;

import com.serenity.monitoring.dto.MonitoringAiCrisisRequest;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoodRiskFeatureServiceTest {

    @Mock
    private EmotionalTriggerRepository emotionalTriggerRepository;

    @InjectMocks
    private MoodRiskFeatureService service;

    @Test
    void buildRequest_handlesNullEntryIdAndDefaults() {
        MoodEntry entry = new MoodEntry();
        entry.setId(null);
        entry.setPatientId(3L);
        entry.setMoodScore(null);
        entry.setMoodDescription(null);
        entry.setTriggers(null);

        MonitoringAiCrisisRequest request = service.buildRequest(entry);

        assertEquals(3L, request.patientId());
        assertEquals(5.0, request.avgMood7days());
        assertEquals(0, request.crisisEntriesCount());
        assertEquals(0.0, request.triggerIntensityAvg());
        assertEquals("", request.moodDescriptionText());
        assertEquals("", request.triggerText());
    }

    @Test
    void buildRequest_buildsCombinedTriggerTextAndRoundsValues() {
        MoodEntry entry = new MoodEntry();
        entry.setId(10L);
        entry.setPatientId(5L);
        entry.setMoodScore(3);
        entry.setMoodDescription("  Feeling overwhelmed  ");
        entry.setTriggers("work,sleep");

        EmotionalTrigger t1 = EmotionalTrigger.builder()
                .triggerType("WORK_STRESS")
                .description("deadline pressure")
                .intensity(3)
                .build();
        EmotionalTrigger t2 = EmotionalTrigger.builder()
                .triggerType("")
                .description("family issue")
                .intensity(4)
                .build();
        when(emotionalTriggerRepository.findByMoodEntryId(10L)).thenReturn(List.of(t1, t2));

        MonitoringAiCrisisRequest request = service.buildRequest(entry);

        assertEquals(5L, request.patientId());
        assertEquals(3.0, request.avgMood7days());
        assertEquals(1, request.crisisEntriesCount());
        assertEquals(3.5, request.triggerIntensityAvg());
        assertEquals(2, request.triggerCount());
        assertEquals("Feeling overwhelmed", request.moodDescriptionText());
        assertEquals("work,sleep || WORK_STRESS: deadline pressure || family issue", request.triggerText());
    }
}
