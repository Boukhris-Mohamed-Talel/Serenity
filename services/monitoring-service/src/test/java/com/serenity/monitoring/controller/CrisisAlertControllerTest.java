package com.serenity.monitoring.controller;

import com.serenity.monitoring.entity.UserAccount;
import com.serenity.monitoring.security.userdetails.CustomUserDetails;
import com.serenity.monitoring.service.CrisisAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrisisAlertControllerTest {

    @Mock
    private CrisisAlertService crisisAlertService;

    @InjectMocks
    private CrisisAlertController controller;

    @Test
    void subscribe_returnsEmitterForSameDoctor() {
        CustomUserDetails principal = principal(8L, "DOCTOR");
        when(crisisAlertService.subscribe(8L)).thenReturn(new SseEmitter());

        SseEmitter emitter = controller.subscribe(8L, principal);

        assertNotNull(emitter);
    }

    @Test
    void subscribe_throwsWhenCurrentUserMissing() {
        assertThrows(AccessDeniedException.class, () -> controller.subscribe(8L, null));
    }

    @Test
    void subscribe_throwsWhenDoctorMismatch() {
        CustomUserDetails principal = principal(9L, "DOCTOR");
        assertThrows(AccessDeniedException.class, () -> controller.subscribe(8L, principal));
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
