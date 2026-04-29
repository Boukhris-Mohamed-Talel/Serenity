package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.CrisisAlertPayload;
import com.serenity.monitoring.dto.WeeklyDoctorDigestPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CrisisAlertServiceImplTest {

    private CrisisAlertServiceImpl service;
    private Map<Long, SseEmitter> emitters;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new CrisisAlertServiceImpl();
        emitters = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(service, "emitters");
    }

    @Test
    void subscribe_registersEmitterForDoctor() {
        SseEmitter emitter = service.subscribe(1L);

        assertNotNull(emitter);
        assertTrue(emitters.containsKey(1L));
    }

    @Test
    void sendCrisisAlert_removesEmitterWhenSendFails() throws IOException {
        SseEmitter failingEmitter = mock(SseEmitter.class);
        doThrow(new IOException("boom"))
                .when(failingEmitter)
                .send(any(SseEmitter.SseEventBuilder.class));
        emitters.put(5L, failingEmitter);

        CrisisAlertPayload payload = CrisisAlertPayload.builder()
                .doctorId(5L)
                .patientId(99L)
                .patientFullName("Patient A")
                .moodLevel(2)
                .message("High risk")
                .timestamp(new Date())
                .build();

        service.sendCrisisAlert(payload);

        verify(failingEmitter).completeWithError(any(IOException.class));
        assertTrue(!emitters.containsKey(5L));
    }

    @Test
    void sendWeeklyDigestNotification_removesEmitterWhenSendFails() throws IOException {
        SseEmitter failingEmitter = mock(SseEmitter.class);
        doThrow(new IOException("boom"))
                .when(failingEmitter)
                .send(any(SseEmitter.SseEventBuilder.class));
        emitters.put(8L, failingEmitter);

        WeeklyDoctorDigestPayload payload = WeeklyDoctorDigestPayload.builder()
                .doctorId(8L)
                .weekStartDate(LocalDate.of(2026, 4, 20))
                .weekEndDate(LocalDate.of(2026, 4, 26))
                .crisisCount(1)
                .worseningPatients(1)
                .noCheckinPatients(1)
                .summaryMessage("recap")
                .generatedAt(new Date())
                .build();

        service.sendWeeklyDigestNotification(payload);

        verify(failingEmitter).completeWithError(any(IOException.class));
        assertTrue(!emitters.containsKey(8L));
    }

    @Test
    void sendCrisisAlert_sendsBothNamedAndFallbackMessages() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        emitters.put(10L, emitter);

        CrisisAlertPayload payload = CrisisAlertPayload.builder()
                .doctorId(10L)
                .patientId(33L)
                .patientFullName("Patient B")
                .moodLevel(3)
                .message("Alert")
                .timestamp(new Date())
                .build();

        service.sendCrisisAlert(payload);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).send(eq(payload), eq(MediaType.APPLICATION_JSON));
    }

    @Test
    void subscribe_replacesPreviousEmitterForSameDoctor() {
        SseEmitter first = service.subscribe(20L);

        SseEmitter second = service.subscribe(20L);

        assertNotNull(second);
        assertTrue(emitters.get(20L) == second);
        assertTrue(emitters.get(20L) != first);
    }

    @Test
    void sendCrisisAlert_ignoresNullPayload() {
        service.sendCrisisAlert(null);

        assertTrue(emitters.isEmpty());
    }

    @Test
    void sendCrisisAlert_ignoresWhenDoctorIdMissing() {
        CrisisAlertPayload payload = CrisisAlertPayload.builder()
                .patientId(33L)
                .message("Alert")
                .build();

        service.sendCrisisAlert(payload);

        assertTrue(emitters.isEmpty());
    }

    @Test
    void sendWeeklyDigestNotification_ignoresWhenNoActiveEmitter() {
        WeeklyDoctorDigestPayload payload = WeeklyDoctorDigestPayload.builder()
                .doctorId(333L)
                .weekStartDate(LocalDate.of(2026, 4, 20))
                .weekEndDate(LocalDate.of(2026, 4, 26))
                .build();
        SseEmitter emitter = mock(SseEmitter.class);

        service.sendWeeklyDigestNotification(payload);

        verifyNoInteractions(emitter);
    }

    @Test
    void sendCrisisAlert_ignoresWhenNoActiveEmitter() {
        CrisisAlertPayload payload = CrisisAlertPayload.builder()
                .doctorId(404L)
                .patientId(1L)
                .build();

        service.sendCrisisAlert(payload);

        assertTrue(!emitters.containsKey(404L));
    }

    @Test
    void sendWeeklyDigestNotification_sendsBothNamedAndFallbackMessages() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        emitters.put(55L, emitter);
        WeeklyDoctorDigestPayload payload = WeeklyDoctorDigestPayload.builder()
                .doctorId(55L)
                .weekStartDate(LocalDate.of(2026, 4, 20))
                .weekEndDate(LocalDate.of(2026, 4, 26))
                .summaryMessage("ok")
                .build();

        service.sendWeeklyDigestNotification(payload);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).send(eq(payload), eq(MediaType.APPLICATION_JSON));
    }

    @Test
    void sendWeeklyDigestNotification_ignoresNullOrMissingDoctorId() {
        service.sendWeeklyDigestNotification(null);
        service.sendWeeklyDigestNotification(WeeklyDoctorDigestPayload.builder().build());

        assertTrue(emitters.isEmpty());
    }
}
