package com.serenity.monitoring.scheduler;

import com.serenity.monitoring.repository.MoodEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoodEntryCleanupSchedulerTest {

    @Mock
    private MoodEntryRepository moodEntryRepository;

    @InjectMocks
    private MoodEntryCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "retentionDays", 1L);
    }

    @Test
    void deleteOldMoodEntriesWithoutClinicalTriggers_skipsWhenNoCandidates() {
        when(moodEntryRepository.countOldEntriesWithoutClinicalTriggers(any(Date.class))).thenReturn(0L);

        scheduler.deleteOldMoodEntriesWithoutClinicalTriggers();

        verify(moodEntryRepository, never()).deleteOldEntriesWithoutClinicalTriggers(any(Date.class));
    }

    @Test
    void deleteOldMoodEntriesWithoutClinicalTriggers_deletesWhenCandidatesExist() {
        when(moodEntryRepository.countOldEntriesWithoutClinicalTriggers(any(Date.class))).thenReturn(3L);
        when(moodEntryRepository.deleteOldEntriesWithoutClinicalTriggers(any(Date.class))).thenReturn(2);

        scheduler.deleteOldMoodEntriesWithoutClinicalTriggers();

        ArgumentCaptor<Date> countCutoff = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> deleteCutoff = ArgumentCaptor.forClass(Date.class);
        verify(moodEntryRepository).countOldEntriesWithoutClinicalTriggers(countCutoff.capture());
        verify(moodEntryRepository).deleteOldEntriesWithoutClinicalTriggers(deleteCutoff.capture());

        assertEquals(countCutoff.getValue(), deleteCutoff.getValue());
    }
}

