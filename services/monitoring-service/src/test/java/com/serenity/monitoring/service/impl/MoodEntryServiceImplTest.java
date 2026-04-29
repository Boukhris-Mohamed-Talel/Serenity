package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.MonitoringAiCrisisRequest;
import com.serenity.monitoring.dto.MonitoringAiCrisisResponse;
import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.dto.MoodEntryResponseDTO;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.integration.MonitoringAiCrisisClient;
import com.serenity.monitoring.mapper.MoodEntryMapper;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.repository.UserProfileSnapshotRepository;
import com.serenity.monitoring.service.CrisisAlertService;
import com.serenity.monitoring.service.DoctorAssignmentService;
import com.serenity.monitoring.service.MoodRiskFeatureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoodEntryServiceImplTest {

    @Mock
    private MoodEntryRepository moodEntryRepository;

    @Mock
    private MoodEntryMapper moodEntryMapper;

    @Mock
    private DoctorAssignmentService doctorAssignmentService;

    @Mock
    private CrisisAlertService crisisAlertService;

    @Mock
    private EmotionalTriggerRepository emotionalTriggerRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserProfileSnapshotRepository userProfileSnapshotRepository;

    @Mock
    private MoodRiskFeatureService moodRiskFeatureService;

    @Mock
    private MonitoringAiCrisisClient monitoringAiCrisisClient;

    @InjectMocks
    private MoodEntryServiceImpl moodEntryService;

    @Test
    void createMoodEntry_setsHighRiskFromAiResponse() {
        MoodEntryRequestDTO request = MoodEntryRequestDTO.builder()
                .patientId(2L)
                .moodScore(9)
                .moodDescription("Feeling fine but I am going to kill myself")
                .triggers("work stress")
                .build();

        when(doctorAssignmentService.getOrAssignDoctor(2L)).thenReturn(99L);
        when(moodEntryMapper.toEntity(any(MoodEntryRequestDTO.class))).thenAnswer(invocation -> {
            MoodEntryRequestDTO dto = invocation.getArgument(0);
            return MoodEntry.builder()
                    .id(1L)
                    .patientId(dto.getPatientId())
                    .doctorId(dto.getDoctorId())
                    .moodScore(dto.getMoodScore())
                    .moodDescription(dto.getMoodDescription())
                    .triggers(dto.getTriggers())
                    .build();
        });
        when(moodEntryRepository.save(any(MoodEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(moodRiskFeatureService.buildRequest(any(MoodEntry.class))).thenReturn(
                new MonitoringAiCrisisRequest(2L, 9.0, 0, 0, 0.0, 0, 9, 0, 1, "", "")
        );
        when(monitoringAiCrisisClient.predict(any(MonitoringAiCrisisRequest.class))).thenReturn(
                Optional.of(new MonitoringAiCrisisResponse(
                        2L,
                        "HIGH_RISK",
                        0.99,
                        "message",
                        "recommendation",
                        3,
                        "SUICIDAL_CRISIS",
                        null
                ))
        );
        when(moodEntryMapper.toResponseDTO(any(MoodEntry.class))).thenReturn(
                MoodEntryResponseDTO.builder()
                        .id(1L)
                        .patientId(2L)
                        .doctorId(99L)
                        .build()
        );
        when(userAccountRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(userProfileSnapshotRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());

        moodEntryService.createMoodEntry(request);

        ArgumentCaptor<MoodEntry> savedCaptor = ArgumentCaptor.forClass(MoodEntry.class);
        verify(moodEntryRepository, times(2)).save(savedCaptor.capture());
        MoodEntry lastSaved = savedCaptor.getAllValues().get(1);

        assertEquals("HIGH_RISK", lastSaved.getAiRiskLevel());
        assertEquals("SUICIDAL_CRISIS", lastSaved.getAiRiskType());
        assertNull(lastSaved.getAiMediumRiskType());
        assertEquals(3, lastSaved.getAiRiskScore());
        assertTrue(lastSaved.getAiRiskConfidence() >= 0.99);
    }

    @Test
    void createMoodEntry_usesMediumRiskTypeFallback() {
        MoodEntryRequestDTO request = MoodEntryRequestDTO.builder()
                .patientId(3L)
                .moodScore(6)
                .moodDescription("Feeling anxious lately")
                .triggers("sleep issues")
                .build();

        when(doctorAssignmentService.getOrAssignDoctor(3L)).thenReturn(77L);
        when(moodEntryMapper.toEntity(any(MoodEntryRequestDTO.class))).thenAnswer(invocation -> {
            MoodEntryRequestDTO dto = invocation.getArgument(0);
            return MoodEntry.builder()
                    .id(2L)
                    .patientId(dto.getPatientId())
                    .doctorId(dto.getDoctorId())
                    .moodScore(dto.getMoodScore())
                    .moodDescription(dto.getMoodDescription())
                    .triggers(dto.getTriggers())
                    .build();
        });
        when(moodEntryRepository.save(any(MoodEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(moodRiskFeatureService.buildRequest(any(MoodEntry.class))).thenReturn(
                new MonitoringAiCrisisRequest(3L, 6.0, 0, 0, 0.0, 0, 6, 1, 1, "", "")
        );
        when(monitoringAiCrisisClient.predict(any(MonitoringAiCrisisRequest.class))).thenReturn(
                Optional.of(new MonitoringAiCrisisResponse(
                        3L,
                        "MEDIUM_RISK",
                        0.81,
                        "message",
                        "recommendation",
                        2,
                        null,
                        "ANXIETY_DISTRESS"
                ))
        );
        when(moodEntryMapper.toResponseDTO(any(MoodEntry.class))).thenReturn(
                MoodEntryResponseDTO.builder()
                        .id(2L)
                        .patientId(3L)
                        .doctorId(77L)
                        .build()
        );
        when(userAccountRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(userProfileSnapshotRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());

        moodEntryService.createMoodEntry(request);

        ArgumentCaptor<MoodEntry> savedCaptor = ArgumentCaptor.forClass(MoodEntry.class);
        verify(moodEntryRepository, times(2)).save(savedCaptor.capture());
        MoodEntry lastSaved = savedCaptor.getAllValues().get(1);

        assertEquals("MEDIUM_RISK", lastSaved.getAiRiskLevel());
        assertEquals("ANXIETY_DISTRESS", lastSaved.getAiRiskType());
        assertEquals("ANXIETY_DISTRESS", lastSaved.getAiMediumRiskType());
        assertEquals(2, lastSaved.getAiRiskScore());
    }

    @Test
    void createMoodEntry_sendsCrisisAlertWhenMoodScoreLow() {
        MoodEntryRequestDTO request = MoodEntryRequestDTO.builder()
                .patientId(5L)
                .moodScore(2)
                .moodDescription("Severe distress")
                .build();
        UserAccount patient = new UserAccount();
        patient.setId(5L);
        patient.setFirstName("Rayen");
        patient.setLastName("B");
        patient.setEmail("rayen@test.com");

        when(doctorAssignmentService.getOrAssignDoctor(5L)).thenReturn(12L);
        when(moodEntryMapper.toEntity(any(MoodEntryRequestDTO.class))).thenAnswer(invocation -> {
            MoodEntryRequestDTO dto = invocation.getArgument(0);
            MoodEntry e = new MoodEntry();
            e.setPatientId(dto.getPatientId());
            e.setDoctorId(dto.getDoctorId());
            e.setMoodScore(dto.getMoodScore());
            return e;
        });
        when(moodEntryRepository.save(any(MoodEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(patient));
        when(moodRiskFeatureService.buildRequest(any(MoodEntry.class))).thenReturn(
                new MonitoringAiCrisisRequest(5L, 2.0, 0, 0, 0.0, 0, 2, 1, 0, "", "")
        );
        when(monitoringAiCrisisClient.predict(any(MonitoringAiCrisisRequest.class))).thenReturn(Optional.empty());
        when(moodEntryMapper.toResponseDTO(any(MoodEntry.class))).thenReturn(
                MoodEntryResponseDTO.builder().id(9L).patientId(5L).doctorId(12L).build()
        );
        when(userAccountRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(userProfileSnapshotRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());

        moodEntryService.createMoodEntry(request);

        verify(crisisAlertService).sendCrisisAlert(any());
        verify(moodEntryRepository, times(1)).save(any(MoodEntry.class));
    }

    @Test
    void updateMoodEntry_preservesDoctorAndPatientOwnership() {
        MoodEntry existing = MoodEntry.builder().id(10L).patientId(30L).doctorId(40L).moodScore(6).build();
        when(moodEntryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(moodEntryRepository.save(any(MoodEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(moodEntryMapper.toResponseDTO(any(MoodEntry.class))).thenReturn(
                MoodEntryResponseDTO.builder().id(10L).patientId(30L).doctorId(40L).build()
        );
        when(userAccountRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(userProfileSnapshotRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());

        MoodEntryRequestDTO request = MoodEntryRequestDTO.builder().patientId(999L).doctorId(888L).moodScore(2).build();
        MoodEntryResponseDTO result = moodEntryService.updateMoodEntry(10L, request);

        assertEquals(30L, result.getPatientId());
        assertEquals(40L, result.getDoctorId());
    }

    @Test
    void deleteMoodEntry_throwsWhenNotFound() {
        when(moodEntryRepository.existsById(404L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> moodEntryService.deleteMoodEntry(404L));
    }

    @Test
    void deleteMoodEntry_throwsWhenClinicalTriggersExist() {
        when(moodEntryRepository.existsById(20L)).thenReturn(true);
        when(emotionalTriggerRepository.existsByMoodEntryId(20L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> moodEntryService.deleteMoodEntry(20L));
        verify(moodEntryRepository, never()).deleteById(20L);
    }

    @Test
    void deleteMoodEntry_deletesWhenNoClinicalTriggers() {
        when(moodEntryRepository.existsById(20L)).thenReturn(true);
        when(emotionalTriggerRepository.existsByMoodEntryId(20L)).thenReturn(false);

        moodEntryService.deleteMoodEntry(20L);

        verify(moodEntryRepository).deleteById(20L);
    }
}
