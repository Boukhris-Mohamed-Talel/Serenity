package com.serenity.monitoring.controller;

import com.serenity.monitoring.dto.ApiResponse;
import com.serenity.monitoring.dto.EmotionalTriggerRequest;
import com.serenity.monitoring.dto.EmotionalTriggerResponse;
import com.serenity.monitoring.service.EmotionalTriggerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmotionalTriggerControllerTest {

    @Mock
    private EmotionalTriggerService emotionalTriggerService;

    @InjectMocks
    private EmotionalTriggerController controller;

    @Test
    void createTrigger_returnsCreatedApiResponse() {
        EmotionalTriggerRequest request = EmotionalTriggerRequest.builder()
                .moodEntryId(5L).triggerType("WORK_STRESS").description("desc sample").intensity(5).build();
        EmotionalTriggerResponse serviceResponse = EmotionalTriggerResponse.builder().id(1L).build();
        when(emotionalTriggerService.createTrigger(5L, request)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<EmotionalTriggerResponse>> result = controller.createTrigger(5L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(true, result.getBody().isSuccess());
        assertEquals(1L, result.getBody().getData().getId());
    }

    @Test
    void getTriggerById_returnsOk() {
        EmotionalTriggerResponse serviceResponse = EmotionalTriggerResponse.builder().id(2L).build();
        when(emotionalTriggerService.getTriggerById(2L)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<EmotionalTriggerResponse>> result = controller.getTriggerById(2L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2L, result.getBody().getData().getId());
    }

    @Test
    void deleteTrigger_returnsOkAndCallsService() {
        ResponseEntity<ApiResponse<Void>> result = controller.deleteTrigger(9L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(emotionalTriggerService).deleteTrigger(9L);
    }

    @Test
    void getTriggersByDoctorId_returnsList() {
        when(emotionalTriggerService.getTriggersByDoctorId(7L))
                .thenReturn(List.of(EmotionalTriggerResponse.builder().id(1L).build()));

        ResponseEntity<ApiResponse<List<EmotionalTriggerResponse>>> result = controller.getTriggersByDoctorId(7L);

        assertEquals(1, result.getBody().getData().size());
    }
}
