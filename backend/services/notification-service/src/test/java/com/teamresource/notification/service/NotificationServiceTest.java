package com.teamresource.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.notification.domain.NotificationChannel;
import com.teamresource.notification.domain.NotificationStatus;
import com.teamresource.notification.domain.NotificationType;
import com.teamresource.notification.infra.client.EventReminderCandidate;
import com.teamresource.notification.infra.persistence.NotificationRecordEntity;
import com.teamresource.notification.infra.persistence.NotificationRecordRepository;
import com.teamresource.notification.infra.persistence.ProcessedEventRepository;
import com.teamresource.notification.service.channel.NotificationSenderFactory;
import com.teamresource.notification.service.channel.NotificationSenderStrategy;
import com.teamresource.notification.service.template.EventReminderTemplateRenderer;
import com.teamresource.notification.service.template.NotificationTemplateRegistry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRecordRepository notificationRecordRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationSenderStrategy inAppSender;

    @Mock
    private NotificationSenderStrategy emailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(inAppSender.channel()).thenReturn(NotificationChannel.IN_APP);
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);
        NotificationSenderFactory notificationSenderFactory = new NotificationSenderFactory(List.of(inAppSender, emailSender));
        NotificationTemplateRegistry templateRegistry = new NotificationTemplateRegistry(List.of());
        EventReminderTemplateRenderer eventReminderTemplateRenderer = new EventReminderTemplateRenderer();

        notificationService = new NotificationService(
                notificationRecordRepository,
                processedEventRepository,
                notificationSenderFactory,
                templateRegistry,
                eventReminderTemplateRenderer,
                new ObjectMapper()
        );
        lenient().doAnswer(invocation -> {
            NotificationRecordEntity record = invocation.getArgument(0);
            record.setStatus(NotificationStatus.SENT);
            record.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
            record.setUpdatedAt(record.getSentAt());
            return null;
        }).when(inAppSender).send(any(NotificationRecordEntity.class));
        lenient().doAnswer(invocation -> {
            NotificationRecordEntity record = invocation.getArgument(0);
            record.setStatus(NotificationStatus.SENT);
            record.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
            record.setUpdatedAt(record.getSentAt());
            return null;
        }).when(emailSender).send(any(NotificationRecordEntity.class));
        lenient().when(notificationRecordRepository.save(any(NotificationRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendEventReminderShouldPersistBothChannels() {
        EventReminderCandidate reminder = new EventReminderCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Team Sync",
                "Room 201",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(2)
        );

        notificationService.sendEventReminder(reminder);

        ArgumentCaptor<NotificationRecordEntity> recordCaptor = ArgumentCaptor.forClass(NotificationRecordEntity.class);
        verify(notificationRecordRepository, times(2)).save(recordCaptor.capture());
        assertThat(recordCaptor.getAllValues())
                .extracting(NotificationRecordEntity::getChannel)
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        assertThat(recordCaptor.getAllValues())
                .extracting(NotificationRecordEntity::getNotificationType)
                .containsOnly(NotificationType.EVENT_REMINDER);
    }

    @Test
    void sendEventReminderShouldIgnoreDuplicateProcessedMarker() {
        EventReminderCandidate reminder = new EventReminderCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Team Sync",
                "Room 201",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(2)
        );
        when(processedEventRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        notificationService.sendEventReminder(reminder);

        verify(notificationRecordRepository, never()).save(any(NotificationRecordEntity.class));
    }

    @Test
    void markReadShouldRejectForeignUser() {
        UUID notificationId = UUID.randomUUID();
        NotificationRecordEntity record = new NotificationRecordEntity();
        record.setNotificationId(notificationId);
        record.setUserId(UUID.randomUUID());

        when(notificationRecordRepository.findById(notificationId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> notificationService.markRead(notificationId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
