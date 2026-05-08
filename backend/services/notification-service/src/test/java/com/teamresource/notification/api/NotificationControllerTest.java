package com.teamresource.notification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.teamresource.notification.api.dto.NotificationResponse;
import com.teamresource.notification.api.dto.PageResponse;
import com.teamresource.notification.api.dto.UnreadCountResponse;
import com.teamresource.notification.application.facade.NotificationFacade;
import com.teamresource.notification.application.service.CurrentUserResolver;
import com.teamresource.notification.application.service.NotificationViewMapper;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.model.NotificationType;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import com.teamresource.notification.service.NotificationService;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationControllerTest {

    private final StubNotificationService notificationService = new StubNotificationService();
    private final StubNotificationFacade notificationFacade = new StubNotificationFacade();
    private final StubNotificationViewMapper notificationViewMapper = new StubNotificationViewMapper();
    private final StubCurrentUserResolver currentUserResolver = new StubCurrentUserResolver();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(
                        notificationService,
                        notificationFacade,
                        notificationViewMapper,
                        currentUserResolver
                ))
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void myNotificationsShouldReturnPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        notificationService.myNotifications = List.of(notificationResponse());

        mockMvc.perform(get("/api/v1/notifications/me").principal((Principal) userId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].subject").value("Upcoming event"));
    }

    @Test
    void unreadCountShouldReturnValue() throws Exception {
        UUID userId = UUID.randomUUID();
        notificationService.unreadCount = new UnreadCountResponse(4);

        mockMvc.perform(get("/api/v1/notifications/me/unread-count").principal((Principal) userId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(4));
    }

    @Test
    void markReadShouldReturnUpdatedNotification() throws Exception {
        UUID userId = UUID.randomUUID();
        notificationService.markReadResponse = notificationResponse();

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", UUID.randomUUID()).principal((Principal) userId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationType").value("EVENT_REMINDER"));
    }

    @Test
    void listNotificationsShouldUseEnhancedFacade() throws Exception {
        UUID userId = UUID.randomUUID();
        currentUserResolver.userId = userId;
        currentUserResolver.admin = false;
        notificationFacade.page = new PageImpl<>(List.of(entity()), PageRequest.of(0, 20), 1);
        notificationViewMapper.response = notificationResponse();
        Authentication authentication = new TestingAuthenticationToken(userId.toString(), null, "ROLE_USER");

        mockMvc.perform(get("/api/v1/notifications").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].subject").value("Upcoming event"));
    }

    static class StubNotificationService extends NotificationService {
        private List<NotificationResponse> myNotifications = List.of();
        private UnreadCountResponse unreadCount = new UnreadCountResponse(0);
        private NotificationResponse markReadResponse;

        StubNotificationService() {
            super(null, null, null, null, null, new ObjectMapper().registerModule(new JavaTimeModule()), new StubIdempotencyHashService());
        }

        @Override
        public List<NotificationResponse> myNotifications(UUID userId, String channel) {
            return myNotifications;
        }

        @Override
        public UnreadCountResponse unreadCount(UUID userId) {
            return unreadCount;
        }

        @Override
        public NotificationResponse markRead(UUID notificationId, UUID userId) {
            return markReadResponse;
        }
    }

    static class StubIdempotencyHashService extends com.teamresource.notification.service.IdempotencyHashService {

        StubIdempotencyHashService() {
            super(null);
        }

        @Override
        public boolean shouldProcess(com.teamresource.notification.infra.messaging.DomainEventMessage eventMessage) {
            return true;
        }
    }

    static class StubNotificationFacade extends NotificationFacade {
        private org.springframework.data.domain.Page<NotificationEntity> page = new PageImpl<>(List.of());

        StubNotificationFacade() {
            super(null, null, null);
        }

        @Override
        public org.springframework.data.domain.Page<NotificationEntity> listForUser(
                UUID userId,
                NotificationStatus status,
                NotificationChannel channel,
                NotificationType type,
                Boolean unreadOnly,
                org.springframework.data.domain.Pageable pageable
        ) {
            return page;
        }
    }

    static class StubNotificationViewMapper extends NotificationViewMapper {
        private NotificationResponse response;

        @Override
        public NotificationResponse toResponse(NotificationEntity entity) {
            return response;
        }
    }

    static class StubCurrentUserResolver extends CurrentUserResolver {
        private UUID userId;
        private boolean admin;

        @Override
        public UUID userId(Authentication authentication) {
            return userId;
        }

        @Override
        public boolean isAdmin(Authentication authentication) {
            return admin;
        }
    }

    private static NotificationEntity entity() {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setCreatedAt(OffsetDateTime.parse("2026-06-01T10:00:00Z"));
        return entity;
    }

    private static NotificationResponse notificationResponse() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        return new NotificationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "event.reminder",
                "EVENT_REMINDER",
                "IN_APP",
                "Upcoming event",
                "Starts soon",
                "SENT",
                null,
                now,
                null,
                now,
                now
        );
    }
}
