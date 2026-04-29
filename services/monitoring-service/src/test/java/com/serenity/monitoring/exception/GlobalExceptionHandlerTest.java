package com.serenity.monitoring.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_returnsNotFoundBody() {
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(new IllegalArgumentException("not found"));

        assertEquals(404, response.getStatusCode().value());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("not found", response.getBody().get("message"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }

    @Test
    void handleIllegalState_returnsConflictBody() {
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalState(new IllegalStateException("conflict"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("conflict", response.getBody().get("message"));
    }

    @Test
    void handleDataIntegrityViolation_returnsConflictBody() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleDataIntegrityViolation(new DataIntegrityViolationException("db"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Operation failed because related records exist.", response.getBody().get("message"));
    }
}
