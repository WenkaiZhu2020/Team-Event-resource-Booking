package com.teamresource.notification.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.notification.api.dto.NotificationResponse;
import com.teamresource.notification.api.dto.UnreadCountResponse;
import com.teamresource.notification.service.NotificationService;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.notification.infra.security.JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(NotificationControllerTest.TestConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubNotificationService notificationService;

    @Test
    void myNotificationsShouldReturnPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        notificationService.notifications = List.of(notificationResponse());

        mockMvc.perform(get("/api/v1/notifications/me").principal(userId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].subject").value("Upcoming event"));
    }

    @Test
    void unreadCountShouldReturnValue() throws Exception {
        UUID userId = UUID.randomUUID();
        notificationService.unreadCountResponse = new UnreadCountResponse(4);

        mockMvc.perform(get("/api/v1/notifications/me/unread-count").principal(userId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(4));
    }

    @Test
    void markReadShouldReturnUpdatedNotification() throws Exception {
        UUID userId = UUID.randomUUID();
        notificationService.markReadResponse = notificationResponse();

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", UUID.randomUUID()).principal(userId::toString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationType").value("EVENT_REMINDER"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubNotificationService notificationService() {
            return new StubNotificationService();
        }
    }

    static class StubNotificationService extends NotificationService {

        private List<NotificationResponse> notifications = List.of();
        private UnreadCountResponse unreadCountResponse = new UnreadCountResponse(0);
        private NotificationResponse markReadResponse;

        StubNotificationService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public List<NotificationResponse> myNotifications(UUID userId, String channel) {
            return notifications;
        }

        @Override
        public UnreadCountResponse unreadCount(UUID userId) {
            return unreadCountResponse;
        }

        @Override
        public NotificationResponse markRead(UUID notificationId, UUID userId) {
            return markReadResponse;
        }
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
