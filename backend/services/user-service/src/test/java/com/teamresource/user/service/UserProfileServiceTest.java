package com.teamresource.user.service;

import com.teamresource.user.api.dto.ProvisionUserRequest;
import com.teamresource.user.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.user.domain.AccountStatus;
import com.teamresource.user.infra.persistence.NotificationPreferenceEntity;
import com.teamresource.user.infra.persistence.NotificationPreferenceRepository;
import com.teamresource.user.infra.persistence.UserProfileEntity;
import com.teamresource.user.infra.persistence.UserProfileRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void provisionShouldCreateProfileAndDefaultPreferences() {
        UUID userId = UUID.randomUUID();
        ProvisionUserRequest request = new ProvisionUserRequest(
                userId,
                " User@Example.com ",
                "Example User",
                "UTC",
                Set.of("RESOURCE_MANAGER", "USER")
        );

        when(userProfileRepository.existsById(userId)).thenReturn(false);
        when(userProfileRepository.save(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationPreferenceRepository.save(any(NotificationPreferenceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userProfileService.provision(request);

        ArgumentCaptor<UserProfileEntity> profileCaptor = ArgumentCaptor.forClass(UserProfileEntity.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(profileCaptor.getValue().getRoleSummary()).isEqualTo("RESOURCE_MANAGER,USER");

        ArgumentCaptor<NotificationPreferenceEntity> preferenceCaptor = ArgumentCaptor.forClass(NotificationPreferenceEntity.class);
        verify(notificationPreferenceRepository).save(preferenceCaptor.capture());
        assertThat(preferenceCaptor.getValue().isInAppEnabled()).isTrue();
        assertThat(preferenceCaptor.getValue().isEmailEnabled()).isTrue();
        assertThat(preferenceCaptor.getValue().getReminderMinutesBefore()).isEqualTo(30);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.roleSummary()).isEqualTo("RESOURCE_MANAGER,USER");
    }

    @Test
    void provisionShouldReturnExistingProfileWhenUserAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UserProfileEntity existing = new UserProfileEntity();
        existing.setUserId(userId);
        existing.setEmail("existing@example.com");
        existing.setDisplayName("Existing");
        existing.setTimezone("UTC");
        existing.setRoleSummary("USER");
        existing.setAccountStatus(AccountStatus.ACTIVE);
        existing.setCreatedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());

        when(userProfileRepository.existsById(userId)).thenReturn(true);
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(existing));

        var response = userProfileService.provision(new ProvisionUserRequest(
                userId,
                "existing@example.com",
                "Existing",
                "UTC",
                Set.of("USER")
        ));

        verify(userProfileRepository, never()).save(any(UserProfileEntity.class));
        verify(notificationPreferenceRepository, never()).save(any(NotificationPreferenceEntity.class));
        assertThat(response.email()).isEqualTo("existing@example.com");
    }

    @Test
    void updatePreferencesShouldPersistNewSettings() {
        UUID userId = UUID.randomUUID();
        NotificationPreferenceEntity preference = new NotificationPreferenceEntity();
        preference.setUserId(userId);
        preference.setInAppEnabled(true);
        preference.setEmailEnabled(true);
        preference.setReminderMinutesBefore(30);
        preference.setUpdatedAt(OffsetDateTime.now());

        when(notificationPreferenceRepository.findById(userId)).thenReturn(Optional.of(preference));
        when(notificationPreferenceRepository.save(any(NotificationPreferenceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userProfileService.updatePreferences(
                userId,
                new UpdateNotificationPreferenceRequest(false, true, 10)
        );

        assertThat(response.inAppEnabled()).isFalse();
        assertThat(response.emailEnabled()).isTrue();
        assertThat(response.reminderMinutesBefore()).isEqualTo(10);
    }
}
