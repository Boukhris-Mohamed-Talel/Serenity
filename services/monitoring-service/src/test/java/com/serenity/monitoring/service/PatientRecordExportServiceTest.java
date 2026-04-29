package com.serenity.monitoring.service;

import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.entity.UserProfileSnapshot;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.repository.UserProfileSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientRecordExportServiceTest {

    @Mock
    private MoodEntryRepository moodEntryRepository;
    @Mock
    private EmotionalTriggerRepository emotionalTriggerRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserProfileSnapshotRepository userProfileSnapshotRepository;

    @InjectMocks
    private PatientRecordExportService service;

    @Test
    void exportDoctorPatientRecordPdf_throwsWhenNoMoodEntries() {
        when(moodEntryRepository.findByDoctorIdAndPatientIdOrderByCreatedAtDesc(1L, 2L)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.exportDoctorPatientRecordPdf(1L, 2L));
    }

    @Test
    void exportDoctorPatientRecordPdf_throwsWhenPatientMissing() {
        MoodEntry entry = moodEntry(100L, 2L, 1L);
        when(moodEntryRepository.findByDoctorIdAndPatientIdOrderByCreatedAtDesc(1L, 2L)).thenReturn(List.of(entry));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.exportDoctorPatientRecordPdf(1L, 2L));
    }

    @Test
    void exportDoctorPatientRecordPdf_returnsPdfBytesForValidData() {
        MoodEntry entry = moodEntry(100L, 2L, 1L);
        UserAccount patient = user(2L, "PATIENT", "Pat", "One", "pat@test.com");
        UserAccount doctor = user(1L, "DOCTOR", "Doc", "One", "doc@test.com");
        UserProfileSnapshot profile = new UserProfileSnapshot();
        profile.setUserId(2L);
        profile.setBio("bio");
        profile.setPreferredLanguage("en");

        EmotionalTrigger trigger = EmotionalTrigger.builder()
                .id(7L)
                .moodEntry(entry)
                .doctorId(1L)
                .triggerType("WORK_STRESS")
                .description("stress")
                .intensity(5)
                .recordedAt(LocalDateTime.now())
                .build();

        when(moodEntryRepository.findByDoctorIdAndPatientIdOrderByCreatedAtDesc(1L, 2L)).thenReturn(List.of(entry));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(userProfileSnapshotRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(emotionalTriggerRepository.findAllForExportByMoodEntryIds(anyList())).thenReturn(List.of(trigger));

        byte[] pdf = service.exportDoctorPatientRecordPdf(1L, 2L);

        assertTrue(pdf.length > 100);
    }

    private MoodEntry moodEntry(Long id, Long patientId, Long doctorId) {
        MoodEntry entry = new MoodEntry();
        entry.setId(id);
        entry.setPatientId(patientId);
        entry.setDoctorId(doctorId);
        entry.setMoodScore(4);
        entry.setMoodDescription("ok");
        entry.setTriggers("none");
        entry.setCreatedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        return entry;
    }

    private UserAccount user(Long id, String role, String first, String last, String email) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setRole(role);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setIsActive(true);
        return user;
    }
}
