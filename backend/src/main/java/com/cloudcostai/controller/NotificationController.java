package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.NotificationResponse;
import com.cloudcostai.dto.UnreadNotificationCountResponse;
import com.cloudcostai.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "In-App Notifications", description = "Endpoints for inspecting and managing in-app cost anomaly notifications and alerts")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List in-app notifications", description = "Retrieve notifications for the current tenant organization, optionally filtered to unread only")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @Parameter(description = "Filter only unread alerts (true/false)")
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @Parameter(description = "Maximum notifications to return (default: 20)")
            @RequestParam(defaultValue = "20") int limit) {
        log.debug("GET /api/notifications - unreadOnly: {}, limit: {}", unreadOnly, limit);
        List<NotificationResponse> list = notificationService.getNotifications(unreadOnly, limit);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count", description = "Retrieve count of unread alert notifications for badge display")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> getUnreadCount() {
        log.debug("GET /api/notifications/unread-count");
        UnreadNotificationCountResponse response = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Transition a single notification to read status")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long id) {
        log.debug("PATCH /api/notifications/{}/read", id);
        NotificationResponse updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", updated));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Transition all unread notifications for the active organization to read")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead() {
        log.info("POST /api/notifications/read-all");
        int count = notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", count));
    }
}

