package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.EmotionalTriggerRequest;
import com.serenity.monitoring.dto.EmotionalTriggerResponse;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.exception.ResourceNotFoundException;
import com.serenity.monitoring.mapper.EmotionalTriggerMapper;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.security.userdetails.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmotionalTriggerServiceImplTest {

    @Mock
    private EmotionalTriggerRepository emotionalTriggerRepository;
    @Mock
    private MoodEntryRepository moodEntryRepository;
    @Mock
    private EmotionalTriggerMapper emotionalTriggerMapper;

    @InjectMocks
    private EmotionalTriggerServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTrigger_createsWhenDoctorOwnsMoodEntry() {
        authenticate(7L, "DOCTOR");
        MoodEntry moodEntry = moodEntry(55L, 101L, 7L);
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(55L)
                .triggerType("WORK_STRESS")
                .description("Work pressure for many days")
                .intensity(8)
                .build();
        EmotionalTrigger entity = EmotionalTrigger.builder().build();
        EmotionalTrigger saved = EmotionalTrigger.builder().id(1L).moodEntry(moodEntry).doctorId(7L).build();
        EmotionalTriggerResponse response = EmotionalTriggerResponse.builder().id(1L).doctorId(7L).moodEntryId(55L).build();

        when(moodEntryRepository.findById(55L)).thenReturn(Optional.of(moodEntry));
        when(emotionalTriggerMapper.toEntity(request)).thenReturn(entity);
        when(emotionalTriggerRepository.save(any(EmotionalTrigger.class))).thenReturn(saved);
        when(emotionalTriggerMapper.toResponse(saved)).thenReturn(response);

        EmotionalTriggerResponse result = service.createTrigger(55L, request);

        assertEquals(1L, result.getId());
        ArgumentCaptor<EmotionalTrigger> captor = ArgumentCaptor.forClass(EmotionalTrigger.class);
        verify(emotionalTriggerRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getDoctorId());
        assertNotNull(captor.getValue().getRecordedAt());
    }

    @Test
    void createTrigger_throwsWhenPathAndBodyIdsMismatch() {
        authenticate(7L, "DOCTOR");
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(99L)
                .triggerType("WORK_STRESS")
                .description("Mismatch ids sample")
                .intensity(5)
                .build();

        assertThrows(IllegalStateException.class, () -> service.createTrigger(55L, request));
    }

    @Test
    void createTrigger_throwsWhenDoctorNotAssigned() {
        authenticate(9L, "DOCTOR");
        MoodEntry moodEntry = moodEntry(55L, 101L, 7L);
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(55L)
                .triggerType("WORK_STRESS")
                .description("Doctor mismatch sample")
                .intensity(5)
                .build();

        when(moodEntryRepository.findById(55L)).thenReturn(Optional.of(moodEntry));

        assertThrows(AccessDeniedException.class, () -> service.createTrigger(55L, request));
    }

    @Test
    void createTrigger_throwsWhenNotAuthenticated() {
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(55L)
                .triggerType("WORK_STRESS")
                .description("No auth context sample")
                .intensity(5)
                .build();

        assertThrows(AccessDeniedException.class, () -> service.createTrigger(55L, request));
    }

    @Test
    void getTriggersByMoodEntryId_returnsForOwnerPatient() {
        authenticate(101L, "PATIENT");
        MoodEntry moodEntry = moodEntry(55L, 101L, 7L);
        EmotionalTrigger trigger = EmotionalTrigger.builder().id(1L).moodEntry(moodEntry).doctorId(7L).build();
        EmotionalTriggerResponse response = EmotionalTriggerResponse.builder().id(1L).moodEntryId(55L).doctorId(7L).build();

        when(moodEntryRepository.findById(55L)).thenReturn(Optional.of(moodEntry));
        when(emotionalTriggerRepository.findByMoodEntryId(55L)).thenReturn(List.of(trigger));
        when(emotionalTriggerMapper.toResponse(trigger)).thenReturn(response);

        List<EmotionalTriggerResponse> result = service.getTriggersByMoodEntryId(55L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getTriggerById_throwsWhenPatientNotOwner() {
        authenticate(999L, "PATIENT");
        MoodEntry moodEntry = moodEntry(55L, 101L, 7L);
        EmotionalTrigger trigger = EmotionalTrigger.builder().id(3L).moodEntry(moodEntry).doctorId(7L).build();

        when(emotionalTriggerRepository.findById(3L)).thenReturn(Optional.of(trigger));

        assertThrows(AccessDeniedException.class, () -> service.getTriggerById(3L));
    }

    @Test
    void getTriggerById_returnsForAssignedDoctor() {
        authenticate(7L, "DOCTOR");
        MoodEntry moodEntry = moodEntry(55L, 101L, 7L);
        EmotionalTrigger trigger = EmotionalTrigger.builder().id(3L).moodEntry(moodEntry).doctorId(7L).build();
        EmotionalTriggerResponse response = EmotionalTriggerResponse.builder().id(3L).doctorId(7L).moodEntryId(55L).build();
        when(emotionalTriggerRepository.findById(3L)).thenReturn(Optional.of(trigger));
        when(emotionalTriggerMapper.toResponse(trigger)).thenReturn(response);

        EmotionalTriggerResponse result = service.getTriggerById(3L);

        assertEquals(3L, result.getId());
    }

    @Test
    void updateTrigger_throwsWhenTriggerNotFound() {
        authenticate(7L, "DOCTOR");
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(55L)
                .triggerType("OTHER")
                .description("Update text sample")
                .intensity(4)
                .build();
        when(emotionalTriggerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateTrigger(404L, request));
    }

    @Test
    void updateTrigger_updatesWhenDoctorOwnsTrigger() {
        authenticate(7L, "DOCTOR");
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(55L)
                .triggerType("OTHER")
                .description("Updated description text")
                .intensity(3)
                .build();
        EmotionalTrigger existing = EmotionalTrigger.builder().id(8L).doctorId(7L).build();
        EmotionalTrigger updated = EmotionalTrigger.builder()
                .id(8L).doctorId(7L).triggerType("OTHER").description("Updated description text").intensity(3).build();
        EmotionalTriggerResponse response = EmotionalTriggerResponse.builder().id(8L).doctorId(7L).build();
        when(emotionalTriggerRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(emotionalTriggerRepository.save(existing)).thenReturn(updated);
        when(emotionalTriggerMapper.toResponse(updated)).thenReturn(response);

        EmotionalTriggerResponse result = service.updateTrigger(8L, request);

        assertEquals(8L, result.getId());
        assertEquals("OTHER", existing.getTriggerType());
        assertEquals(3, existing.getIntensity());
    }

    @Test
    void updateTrigger_throwsWhenDoctorDoesNotOwnTrigger() {
        authenticate(7L, "DOCTOR");
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(55L)
                .triggerType("OTHER")
                .description("Will fail")
                .intensity(3)
                .build();
        EmotionalTrigger existing = EmotionalTrigger.builder().id(8L).doctorId(99L).build();
        when(emotionalTriggerRepository.findById(8L)).thenReturn(Optional.of(existing));

        assertThrows(AccessDeniedException.class, () -> service.updateTrigger(8L, request));
    }

    @Test
    void getTriggersByDoctorId_throwsWhenDoctorRequestsAnotherDoctorData() {
        authenticate(7L, "DOCTOR");

        assertThrows(AccessDeniedException.class, () -> service.getTriggersByDoctorId(99L));
    }

    @Test
    void getTriggersByDoctorId_returnsOwnTriggers() {
        authenticate(7L, "DOCTOR");
        EmotionalTrigger trigger = EmotionalTrigger.builder().id(1L).doctorId(7L).build();
        EmotionalTriggerResponse response = EmotionalTriggerResponse.builder().id(1L).doctorId(7L).build();
        when(emotionalTriggerRepository.findByDoctorId(7L)).thenReturn(List.of(trigger));
        when(emotionalTriggerMapper.toResponse(trigger)).thenReturn(response);

        List<EmotionalTriggerResponse> result = service.getTriggersByDoctorId(7L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void deleteTrigger_deletesWhenDoctorOwnsTrigger() {
        authenticate(7L, "DOCTOR");
        EmotionalTrigger trigger = EmotionalTrigger.builder().id(6L).doctorId(7L).build();
        when(emotionalTriggerRepository.findById(6L)).thenReturn(Optional.of(trigger));

        service.deleteTrigger(6L);

        verify(emotionalTriggerRepository).delete(trigger);
    }

    @Test
    void deleteTrigger_throwsWhenDoctorDoesNotOwnTrigger() {
        authenticate(7L, "DOCTOR");
        EmotionalTrigger trigger = EmotionalTrigger.builder().id(6L).doctorId(99L).build();
        when(emotionalTriggerRepository.findById(6L)).thenReturn(Optional.of(trigger));

        assertThrows(AccessDeniedException.class, () -> service.deleteTrigger(6L));
        verify(emotionalTriggerRepository, never()).delete(any(EmotionalTrigger.class));
    }

    private void authenticate(Long userId, String role) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setEmail("user@x.com");
        user.setRole(role);
        user.setIsActive(true);
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private MoodEntry moodEntry(Long id, Long patientId, Long doctorId) {
        MoodEntry entry = new MoodEntry();
        entry.setId(id);
        entry.setPatientId(patientId);
        entry.setDoctorId(doctorId);
        return entry;
    }
}
