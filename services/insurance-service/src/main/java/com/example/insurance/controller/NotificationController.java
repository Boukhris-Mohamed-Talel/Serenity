package com.example.insurance.controller;

import com.example.insurance.dto.InsuranceNotificationResponseDTO;
import com.example.insurance.dto.NotificationUnreadCountDTO;
import com.example.insurance.security.InsuranceAuth;
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
    public ResponseEntity<List<InsuranceNotificationResponseDTO>> getMyNotifications() {
        Long userId = InsuranceAuth.requireUserId();
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<NotificationUnreadCountDTO> getUnreadCount() {
        Long userId = InsuranceAuth.requireUserId();
        return ResponseEntity.ok(new NotificationUnreadCountDTO(notificationService.countUnread(userId)));
    }

    @PatchMapping("/me/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        Long userId = InsuranceAuth.requireUserId();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Long userId = InsuranceAuth.requireUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<InsuranceNotificationResponseDTO>> getAllNotificationsForAdmin() {
        InsuranceAuth.requireAdmin();
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }
}
