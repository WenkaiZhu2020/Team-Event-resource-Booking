package com.teamresource.notification.api;

import com.teamresource.notification.api.dto.ApiResponse;
import com.teamresource.notification.api.dto.BatchReadRequest;
import com.teamresource.notification.api.dto.NotificationResponse;
import com.teamresource.notification.api.dto.PageResponse;
import com.teamresource.notification.api.dto.UnreadCountResponse;
import com.teamresource.notification.application.service.CurrentUserResolver;
import com.teamresource.notification.application.service.NotificationViewMapper;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.model.NotificationType;
import com.teamresource.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired(required = false)
    private com.teamresource.notification.application.facade.NotificationFacade enhancedFacade;

    @Autowired(required = false)
    private NotificationViewMapper notificationViewMapper;

    @Autowired(required = false)
    private CurrentUserResolver currentUserResolver;

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

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> listNotifications(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        requireEnhancedFacade();
        UUID userId = currentUserResolver.userId(authentication);
        Page<NotificationResponse> result = enhancedFacade.listForUser(
                        userId,
                        status,
                        channel,
                        type,
                        unreadOnly,
                        PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(notificationViewMapper::toResponse);
        return ApiResponse.of(new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        ));
    }

    @GetMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> getById(@PathVariable UUID notificationId, Authentication authentication) {
        requireEnhancedFacade();
        UUID userId = currentUserResolver.userId(authentication);
        boolean admin = currentUserResolver.isAdmin(authentication);
        return ApiResponse.of(notificationViewMapper.toResponse(enhancedFacade.getById(userId, notificationId, admin)));
    }

    @GetMapping("/me/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(Principal principal) {
        return ApiResponse.of(notificationService.unreadCount(parsePrincipal(principal)));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID notificationId, Principal principal) {
        return ApiResponse.of(notificationService.markRead(notificationId, parsePrincipal(principal)));
    }

    @PostMapping("/read-batch")
    public ApiResponse<Integer> markReadBatch(@Valid @RequestBody BatchReadRequest request, Authentication authentication) {
        requireEnhancedFacade();
        UUID userId = currentUserResolver.userId(authentication);
        boolean admin = currentUserResolver.isAdmin(authentication);
        return ApiResponse.of(enhancedFacade.markReadBatch(userId, request.notificationIds(), admin));
    }

    private UUID parsePrincipal(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    private void requireEnhancedFacade() {
        if (enhancedFacade == null || notificationViewMapper == null || currentUserResolver == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Notification query layer is unavailable");
        }
    }
}
