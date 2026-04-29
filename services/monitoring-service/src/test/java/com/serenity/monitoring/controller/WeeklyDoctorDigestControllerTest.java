package com.serenity.monitoring.controller;

import com.serenity.monitoring.dto.WeeklyDoctorDigestResponseDTO;
import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.security.userdetails.CustomUserDetails;
import com.serenity.monitoring.service.WeeklyDoctorDigestService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyDoctorDigestControllerTest {

    @Mock
    private WeeklyDoctorDigestService weeklyDoctorDigestService;

    @InjectMocks
    private WeeklyDoctorDigestController controller;

    @Test
    void getLatestDigest_returnsOkForOwnerDoctor() {
        CustomUserDetails principal = principal(5L, "DOCTOR");
        when(weeklyDoctorDigestService.getLatestDigestForDoctor(5L))
                .thenReturn(WeeklyDoctorDigestResponseDTO.builder().doctorId(5L).build());

        ResponseEntity<WeeklyDoctorDigestResponseDTO> result = controller.getLatestDigest(5L, principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(5L, result.getBody().getDoctorId());
    }

    @Test
    void getRecentDigests_throwsWhenNotOwner() {
        CustomUserDetails principal = principal(9L, "DOCTOR");

        assertThrows(AccessDeniedException.class, () -> controller.getRecentDigests(5L, principal));
    }

    @Test
    void getRecentDigests_returnsOkForOwner() {
        CustomUserDetails principal = principal(5L, "DOCTOR");
        when(weeklyDoctorDigestService.getRecentDigestsForDoctor(5L))
                .thenReturn(List.of(WeeklyDoctorDigestResponseDTO.builder().doctorId(5L).build()));

        ResponseEntity<List<WeeklyDoctorDigestResponseDTO>> result = controller.getRecentDigests(5L, principal);

        assertEquals(1, result.getBody().size());
    }

    @Test
    void runNow_returnsAccepted() {
        ResponseEntity<Void> result = controller.runNow();
        assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
        verify(weeklyDoctorDigestService).generateWeeklyDigests();
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
