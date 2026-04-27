package com.teamresource.notification.api;

import com.teamresource.notification.api.dto.ApiResponse;
import com.teamresource.notification.api.dto.NotificationResponse;
import com.teamresource.notification.api.dto.UnreadCountResponse;
import com.teamresource.notification.service.NotificationService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public ApiResponse<List<NotificationResponse>> myNotifications(
            Principal principal,
            @RequestParam(required = false) String channel
    ) {
        return ApiResponse.of(notificationService.myNotifications(parsePrincipal(principal), channel));
    }

    @GetMapping("/me/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(Principal principal) {
        return ApiResponse.of(notificationService.unreadCount(parsePrincipal(principal)));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID notificationId, Principal principal) {
        return ApiResponse.of(notificationService.markRead(notificationId, parsePrincipal(principal)));
    }

    private UUID parsePrincipal(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }
}
