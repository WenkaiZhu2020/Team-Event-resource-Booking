package com.teamresource.notification.api.controller;

import com.teamresource.notification.api.dto.ApiResponse;
import com.teamresource.notification.api.dto.BatchReadRequest;
import com.teamresource.notification.api.dto.NotificationResponse;
import com.teamresource.notification.api.dto.PageResponse;
import com.teamresource.notification.application.facade.NotificationFacade;
import com.teamresource.notification.application.service.CurrentUserResolver;
import com.teamresource.notification.application.service.NotificationViewMapper;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.model.NotificationType;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Profile("source-architecture")
public class NotificationController {

    private final NotificationFacade facade;
    private final NotificationViewMapper viewMapper;
    private final CurrentUserResolver currentUserResolver;

    public NotificationController(
            NotificationFacade facade,
            NotificationViewMapper viewMapper,
            CurrentUserResolver currentUserResolver
    ) {
        this.facade = facade;
        this.viewMapper = viewMapper;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> listMyNotifications(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        UUID userId = currentUserResolver.userId(authentication);

        Page<NotificationResponse> result = facade.listForUser(
                        userId,
                        status,
                        channel,
                        type,
                        unreadOnly,
                        PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(viewMapper::toResponse);

        PageResponse<NotificationResponse> response = new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID userId = currentUserResolver.userId(authentication);
        boolean admin = currentUserResolver.isAdmin(authentication);
        NotificationResponse response = viewMapper.toResponse(facade.getById(userId, notificationId, admin));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID userId = currentUserResolver.userId(authentication);
        boolean admin = currentUserResolver.isAdmin(authentication);
        NotificationResponse response = viewMapper.toResponse(facade.markRead(userId, notificationId, admin));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/read-batch")
    public ResponseEntity<ApiResponse<Integer>> markReadBatch(
            @Valid @RequestBody BatchReadRequest request,
            Authentication authentication
    ) {
        UUID userId = currentUserResolver.userId(authentication);
        boolean admin = currentUserResolver.isAdmin(authentication);
        int updated = facade.markReadBatch(userId, request.notificationIds(), admin);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }
}
