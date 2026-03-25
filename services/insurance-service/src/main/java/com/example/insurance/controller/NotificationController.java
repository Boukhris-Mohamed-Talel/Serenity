package com.example.insurance.controller;

import com.example.insurance.dto.InsuranceNotificationResponseDTO;
import com.example.insurance.dto.NotificationUnreadCountDTO;
import com.example.insurance.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    public ResponseEntity<List<InsuranceNotificationResponseDTO>> getMyNotifications(
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<NotificationUnreadCountDTO> getUnreadCount(
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        return ResponseEntity.ok(new NotificationUnreadCountDTO(notificationService.countUnread(userId)));
    }

    @PatchMapping("/me/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = true) Long userId
    ) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<InsuranceNotificationResponseDTO>> getAllNotificationsForAdmin() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }
}

