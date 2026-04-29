package com.teamresource.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamresource.notification.application.facade.NotificationFacade;
import com.teamresource.notification.application.pipeline.NotificationDispatchCommand;
import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.model.NotificationStatus;
import com.teamresource.notification.domain.model.NotificationType;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import com.teamresource.notification.infrastructure.persistence.repository.SpringDataNotificationJpaRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotificationDispatchServiceIntegrationTest {

    @Autowired
    private NotificationFacade notificationFacade;

    @Autowired
    private SpringDataNotificationJpaRepository notificationJpaRepository;

    @Test
    void shouldDispatchAndRemainIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        NotificationDispatchCommand command = new NotificationDispatchCommand(
                userId,
                NotificationType.BOOKING_CONFIRMED,
                "BOOKING_CONFIRMED",
                "booking-domain",
                "BOOKING_CONFIRMED",
                "BOOKING",
                bookingId,
                "msg-1001",
                Map.of(
                        "bookingId", bookingId.toString(),
                        "resourceId", UUID.randomUUID().toString(),
                        "startAt", startAt.toString(),
                        "endAt", startAt.plusHours(1).toString()),
                List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                null,
                3
        );

        List<NotificationEntity> first = notificationFacade.dispatch(command);
        List<NotificationEntity> second = notificationFacade.dispatch(command);

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);

        List<NotificationEntity> persisted = notificationJpaRepository.findAll();
        assertThat(persisted).hasSize(2);
        assertThat(persisted).extracting(NotificationEntity::getStatus)
                .containsOnly(NotificationStatus.SENT);
    }
}
