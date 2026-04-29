package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.WeeklyDoctorDigestPayload;
import com.serenity.monitoring.dto.WeeklyDoctorDigestResponseDTO;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.entity.WeeklyDoctorDigest;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import com.serenity.monitoring.repository.WeeklyDoctorDigestRepository;
import com.serenity.monitoring.service.CrisisAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyDoctorDigestServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private MoodEntryRepository moodEntryRepository;
    @Mock
    private WeeklyDoctorDigestRepository weeklyDoctorDigestRepository;
    @Mock
    private CrisisAlertService crisisAlertService;

    @InjectMocks
    private WeeklyDoctorDigestServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "digestTimezone", "UTC");
    }

    @Test
    void generateWeeklyDigests_skipsWhenNoDoctors() {
        when(userAccountRepository.findAllByRoleAndIsActiveTrueOrderByIdAsc("DOCTOR")).thenReturn(List.of());

        service.generateWeeklyDigests();

        verify(weeklyDoctorDigestRepository, never()).save(any(WeeklyDoctorDigest.class));
        verify(crisisAlertService, never()).sendWeeklyDigestNotification(any(WeeklyDoctorDigestPayload.class));
    }

    @Test
    void generateWeeklyDigests_skipsWhenDigestAlreadyExists() {
        UserAccount doctor = doctor(11L);
        LocalDate weekStart = weekStart("UTC");
        when(userAccountRepository.findAllByRoleAndIsActiveTrueOrderByIdAsc("DOCTOR")).thenReturn(List.of(doctor));
        when(weeklyDoctorDigestRepository.existsByDoctorIdAndWeekStartDate(11L, weekStart)).thenReturn(true);

        service.generateWeeklyDigests();

        verify(weeklyDoctorDigestRepository, never()).save(any(WeeklyDoctorDigest.class));
        verify(crisisAlertService, never()).sendWeeklyDigestNotification(any(WeeklyDoctorDigestPayload.class));
    }

    @Test
    void generateWeeklyDigests_buildsDigestAndSendsNotification() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate weekStart = weekStart("UTC");
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate previousWeekStart = weekStart.minusWeeks(1);

        UserAccount doctor = doctor(10L);
        MoodEntry currentCritical = moodEntry(10L, 100L, 2, atStart(weekStart, zoneId));
        MoodEntry previousP100 = moodEntry(10L, 100L, 10, atStart(previousWeekStart, zoneId));
        MoodEntry previousP200 = moodEntry(10L, 200L, 8, atStart(previousWeekStart.plusDays(1), zoneId));

        when(userAccountRepository.findAllByRoleAndIsActiveTrueOrderByIdAsc("DOCTOR")).thenReturn(List.of(doctor));
        when(weeklyDoctorDigestRepository.existsByDoctorIdAndWeekStartDate(10L, weekStart)).thenReturn(false);
        when(moodEntryRepository.findByDoctorIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(currentCritical, previousP100, previousP200));
        when(weeklyDoctorDigestRepository.save(any(WeeklyDoctorDigest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.generateWeeklyDigests();

        ArgumentCaptor<WeeklyDoctorDigest> digestCaptor = ArgumentCaptor.forClass(WeeklyDoctorDigest.class);
        verify(weeklyDoctorDigestRepository).save(digestCaptor.capture());
        WeeklyDoctorDigest digest = digestCaptor.getValue();

        assertEquals(10L, digest.getDoctorId());
        assertEquals(weekStart, digest.getWeekStartDate());
        assertEquals(weekEnd, digest.getWeekEndDate());
        assertEquals(1, digest.getCrisisCount());
        assertEquals(1, digest.getWorseningPatients());
        assertEquals(1, digest.getNoCheckinPatients());
        assertNotNull(digest.getGeneratedAt());

        ArgumentCaptor<WeeklyDoctorDigestPayload> payloadCaptor = ArgumentCaptor.forClass(WeeklyDoctorDigestPayload.class);
        verify(crisisAlertService).sendWeeklyDigestNotification(payloadCaptor.capture());
        assertEquals(10L, payloadCaptor.getValue().getDoctorId());
    }

    @Test
    void getLatestDigestForDoctor_returnsNullWhenMissing() {
        when(weeklyDoctorDigestRepository.findTopByDoctorIdOrderByWeekStartDateDesc(7L)).thenReturn(Optional.empty());

        WeeklyDoctorDigestResponseDTO result = service.getLatestDigestForDoctor(7L);

        assertNull(result);
    }

    @Test
    void getRecentDigestsForDoctor_mapsDtos() {
        WeeklyDoctorDigest digest = WeeklyDoctorDigest.builder()
                .id(5L)
                .doctorId(90L)
                .weekStartDate(LocalDate.of(2026, 4, 20))
                .weekEndDate(LocalDate.of(2026, 4, 26))
                .crisisCount(2)
                .worseningPatients(1)
                .noCheckinPatients(3)
                .summaryMessage("summary")
                .generatedAt(new Date())
                .build();
        when(weeklyDoctorDigestRepository.findTop12ByDoctorIdOrderByWeekStartDateDesc(90L)).thenReturn(List.of(digest));

        List<WeeklyDoctorDigestResponseDTO> result = service.getRecentDigestsForDoctor(90L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getId());
        assertEquals(90L, result.get(0).getDoctorId());
        assertEquals(2, result.get(0).getCrisisCount());
    }

    private UserAccount doctor(Long id) {
        UserAccount doctor = new UserAccount();
        doctor.setId(id);
        doctor.setRole("DOCTOR");
        doctor.setIsActive(true);
        return doctor;
    }

    private MoodEntry moodEntry(Long doctorId, Long patientId, Integer moodScore, Date createdAt) {
        MoodEntry entry = new MoodEntry();
        entry.setDoctorId(doctorId);
        entry.setPatientId(patientId);
        entry.setMoodScore(moodScore);
        entry.setCreatedAt(createdAt);
        return entry;
    }

    private LocalDate weekStart(String zone) {
        ZoneId zoneId = ZoneId.of(zone);
        LocalDate thisWeekMonday = LocalDate.now(zoneId).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisWeekMonday.minusWeeks(1);
    }

    private Date atStart(LocalDate day, ZoneId zoneId) {
        return Date.from(day.atStartOfDay(zoneId).toInstant());
    }
}
