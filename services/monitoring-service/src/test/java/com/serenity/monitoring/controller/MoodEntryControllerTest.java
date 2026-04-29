package com.serenity.monitoring.controller;

import com.serenity.monitoring.dto.MoodEntryRequestDTO;
import com.serenity.monitoring.dto.MoodEntryResponseDTO;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.security.userdetails.CustomUserDetails;
import com.serenity.monitoring.service.MoodEntryService;
import com.serenity.monitoring.service.PatientRecordExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoodEntryControllerTest {

    @Mock
    private MoodEntryService moodEntryService;
    @Mock
    private PatientRecordExportService patientRecordExportService;

    @InjectMocks
    private MoodEntryController controller;

    @Test
    void createMoodEntry_returnsCreated() {
        MoodEntryRequestDTO request = MoodEntryRequestDTO.builder().patientId(2L).moodScore(5).build();
        MoodEntryResponseDTO response = MoodEntryResponseDTO.builder().id(1L).patientId(2L).build();
        when(moodEntryService.createMoodEntry(request)).thenReturn(response);

        ResponseEntity<MoodEntryResponseDTO> result = controller.createMoodEntry(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void getMoodEntriesByPatient_returnsOk() {
        when(moodEntryService.getMoodEntriesByPatient(2L)).thenReturn(List.of(MoodEntryResponseDTO.builder().id(1L).build()));

        ResponseEntity<List<MoodEntryResponseDTO>> result = controller.getMoodEntriesByPatient(2L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getMoodEntriesByDoctor_returnsOk() {
        when(moodEntryService.getMoodEntriesByDoctor(7L)).thenReturn(List.of(MoodEntryResponseDTO.builder().id(2L).build()));

        ResponseEntity<List<MoodEntryResponseDTO>> result = controller.getMoodEntriesByDoctor(7L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getMoodEntryById_returnsOk() {
        when(moodEntryService.getMoodEntryById(3L)).thenReturn(MoodEntryResponseDTO.builder().id(3L).build());

        ResponseEntity<MoodEntryResponseDTO> result = controller.getMoodEntryById(3L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3L, result.getBody().getId());
    }

    @Test
    void updateMoodEntry_returnsOk() {
        MoodEntryRequestDTO request = MoodEntryRequestDTO.builder().patientId(2L).moodScore(5).moodDescription("desc text 01").build();
        when(moodEntryService.updateMoodEntry(3L, request)).thenReturn(MoodEntryResponseDTO.builder().id(3L).build());

        ResponseEntity<MoodEntryResponseDTO> result = controller.updateMoodEntry(3L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3L, result.getBody().getId());
    }

    @Test
    void deleteMoodEntry_returnsNoContent() {
        ResponseEntity<Void> result = controller.deleteMoodEntry(10L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(moodEntryService).deleteMoodEntry(10L);
    }

    @Test
    void exportPatientRecordPdf_throwsWhenDoctorMismatch() {
        CustomUserDetails currentUser = principal(77L, "DOCTOR");

        assertThrows(AccessDeniedException.class, () -> controller.exportPatientRecordPdf(1L, 2L, currentUser));
    }

    @Test
    void exportPatientRecordPdf_returnsAttachmentForOwner() {
        CustomUserDetails currentUser = principal(7L, "DOCTOR");
        when(patientRecordExportService.exportDoctorPatientRecordPdf(7L, 2L)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> result = controller.exportPatientRecordPdf(7L, 2L, currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().length);
        assertTrue(result.getHeaders().getFirst("Content-Disposition").contains("attachment"));
    }

    private CustomUserDetails principal(Long id, String role) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setRole(role);
        user.setEmail("u@test.com");
        user.setIsActive(true);
        return new CustomUserDetails(user);
    }
}
