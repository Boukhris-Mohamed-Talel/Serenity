package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.DoctorDTO;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAssignmentServiceImplTest {

    @Mock
    private MoodEntryRepository moodEntryRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Spy
    @InjectMocks
    private DoctorAssignmentServiceImpl service;

    @Test
    void getOrAssignDoctor_returnsExistingDoctor_whenPatientAlreadyAssigned() {
        MoodEntry existing = new MoodEntry();
        existing.setDoctorId(44L);
        when(moodEntryRepository.findFirstByPatientIdOrderByCreatedAtAsc(11L)).thenReturn(Optional.of(existing));

        Long doctorId = service.getOrAssignDoctor(11L);

        assertEquals(44L, doctorId);
    }

    @Test
    void getOrAssignDoctor_assignsNewDoctor_whenNoExistingEntry() {
        when(moodEntryRepository.findFirstByPatientIdOrderByCreatedAtAsc(15L)).thenReturn(Optional.empty());
        doReturn(77L).when(service).assignDoctorToPatient(15L);

        Long doctorId = service.getOrAssignDoctor(15L);

        assertEquals(77L, doctorId);
        verify(service).assignDoctorToPatient(15L);
    }

    @Test
    void getOrAssignDoctor_assignsNewDoctor_whenExistingEntryHasNullDoctor() {
        MoodEntry existing = new MoodEntry();
        existing.setDoctorId(null);
        when(moodEntryRepository.findFirstByPatientIdOrderByCreatedAtAsc(20L)).thenReturn(Optional.of(existing));
        doReturn(88L).when(service).assignDoctorToPatient(20L);

        Long doctorId = service.getOrAssignDoctor(20L);

        assertEquals(88L, doctorId);
        verify(service).assignDoctorToPatient(20L);
    }

    @Test
    void assignDoctorToPatient_selectsDoctorWithLeastDistinctPatients() {
        doReturn(List.of(
                DoctorDTO.builder().id(1L).build(),
                DoctorDTO.builder().id(2L).build(),
                DoctorDTO.builder().id(3L).build()
        )).when(service).getAllDoctors();
        when(moodEntryRepository.countDistinctPatientsByDoctorId(1L)).thenReturn(5L);
        when(moodEntryRepository.countDistinctPatientsByDoctorId(2L)).thenReturn(2L);
        when(moodEntryRepository.countDistinctPatientsByDoctorId(3L)).thenReturn(3L);

        Long assigned = service.assignDoctorToPatient(99L);

        assertEquals(2L, assigned);
    }

    @Test
    void assignDoctorToPatient_throwsWhenNoDoctorsAvailable() {
        doReturn(List.of()).when(service).getAllDoctors();

        assertThrows(IllegalStateException.class, () -> service.assignDoctorToPatient(1L));
    }

    @Test
    void getAllDoctors_mapsActiveDoctors() {
        UserAccount d1 = new UserAccount();
        d1.setId(7L);
        d1.setFirstName("John");
        d1.setLastName("Doe");
        d1.setEmail("john@hospital.com");
        d1.setRole("DOCTOR");
        d1.setIsActive(true);

        when(userAccountRepository.findAllByRoleAndIsActiveTrueOrderByIdAsc("DOCTOR"))
                .thenReturn(List.of(d1));

        List<DoctorDTO> result = service.getAllDoctors();

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getId());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Doe", result.get(0).getLastName());
        assertEquals("john@hospital.com", result.get(0).getEmail());
        assertEquals("DOCTOR", result.get(0).getRole());
    }

    @Test
    void getAllDoctors_throwsWhenNoneFound() {
        when(userAccountRepository.findAllByRoleAndIsActiveTrueOrderByIdAsc("DOCTOR"))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.getAllDoctors());
    }
}
