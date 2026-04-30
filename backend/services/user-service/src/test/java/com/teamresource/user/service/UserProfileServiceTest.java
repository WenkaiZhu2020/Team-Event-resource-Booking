package com.teamresource.user.service;

import com.teamresource.user.api.dto.ProvisionUserRequest;
import com.teamresource.user.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.user.api.dto.UpdateUserProfileRequest;
import com.teamresource.user.domain.AccountStatus;
import com.teamresource.user.infra.persistence.NotificationPreferenceEntity;
import com.teamresource.user.infra.persistence.NotificationPreferenceRepository;
import com.teamresource.user.infra.persistence.UserProfileEntity;
import com.teamresource.user.infra.persistence.UserProfileRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationPreferenceRepository.existsById(userId)).thenReturn(false);
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
        existing.setEmail("old@example.com");
        existing.setDisplayName("Old");
        existing.setTimezone("America/New_York");
        existing.setRoleSummary("USER");
        existing.setAccountStatus(AccountStatus.ACTIVE);
        existing.setCreatedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        NotificationPreferenceEntity existingPreference = new NotificationPreferenceEntity();
        existingPreference.setUserId(userId);

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationPreferenceRepository.existsById(userId)).thenReturn(true);

        var response = userProfileService.provision(new ProvisionUserRequest(
                userId,
                "existing@example.com",
                "Existing",
                "UTC",
                Set.of("RESOURCE_MANAGER", "USER")
        ));

        verify(userProfileRepository).save(any(UserProfileEntity.class));
        verify(notificationPreferenceRepository, never()).save(any(NotificationPreferenceEntity.class));
        assertThat(response.email()).isEqualTo("existing@example.com");
        assertThat(response.displayName()).isEqualTo("Existing");
        assertThat(response.timezone()).isEqualTo("UTC");
        assertThat(response.roleSummary()).isEqualTo("RESOURCE_MANAGER,USER");
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

    @Test
    void syncRolesShouldUpdateRoleSummary() {
        UUID userId = UUID.randomUUID();
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        profile.setEmail("user@example.com");
        profile.setDisplayName("User");
        profile.setTimezone("UTC");
        profile.setRoleSummary("USER");
        profile.setAccountStatus(AccountStatus.ACTIVE);
        profile.setCreatedAt(OffsetDateTime.now());
        profile.setUpdatedAt(OffsetDateTime.now());

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userProfileService.syncRoles(userId, Set.of("admin", "user"));

        assertThat(response.roleSummary()).isEqualTo("ADMIN,USER");
    }

    @Test
    void updateProfileShouldPersistTrimmedValues() {
        UUID userId = UUID.randomUUID();
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        profile.setEmail("user@example.com");
        profile.setDisplayName("User");
        profile.setTimezone("UTC");
        profile.setRoleSummary("USER");
        profile.setAccountStatus(AccountStatus.ACTIVE);
        profile.setCreatedAt(OffsetDateTime.now());
        profile.setUpdatedAt(OffsetDateTime.now());

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userProfileService.updateProfile(userId, new UpdateUserProfileRequest(" Updated ", " Europe/Dublin "));

        assertThat(response.displayName()).isEqualTo("Updated");
        assertThat(response.timezone()).isEqualTo("Europe/Dublin");
    }

    @Test
    void provisionShouldRejectEmptyRoles() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.provision(new ProvisionUserRequest(
                userId,
                "user@example.com",
                "User",
                "UTC",
                Collections.emptySet()
        ))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void provisionShouldRejectBlankRolesAfterNormalization() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.provision(new ProvisionUserRequest(
                userId,
                "user@example.com",
                "User",
                "UTC",
                Set.of("   ")
        ))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void provisionShouldRejectNullRoles() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.provision(new ProvisionUserRequest(
                userId,
                "user@example.com",
                "User",
                "UTC",
                null
        ))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void syncRolesShouldRejectMissingProfile() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.syncRoles(userId, Set.of("USER")))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void meShouldReturnProfile() {
        UUID userId = UUID.randomUUID();
        UserProfileEntity profile = activeProfile(userId);
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        var response = userProfileService.me(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    void meShouldRejectMissingProfile() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.me(userId))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void updateProfileShouldRejectMissingProfile() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.updateProfile(userId, new UpdateUserProfileRequest("User", "UTC")))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void preferencesShouldReturnCurrentSettings() {
        UUID userId = UUID.randomUUID();
        NotificationPreferenceEntity preference = preference(userId);
        when(notificationPreferenceRepository.findById(userId)).thenReturn(Optional.of(preference));

        var response = userProfileService.preferences(userId);

        assertThat(response.inAppEnabled()).isTrue();
        assertThat(response.emailEnabled()).isTrue();
        assertThat(response.reminderMinutesBefore()).isEqualTo(30);
    }

    @Test
    void preferencesShouldRejectMissingPreference() {
        UUID userId = UUID.randomUUID();
        when(notificationPreferenceRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.preferences(userId))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void updatePreferencesShouldRejectMissingPreference() {
        UUID userId = UUID.randomUUID();
        when(notificationPreferenceRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.updatePreferences(
                userId,
                new UpdateNotificationPreferenceRequest(true, false, 10)
        )).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    private UserProfileEntity activeProfile(UUID userId) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        profile.setEmail("user@example.com");
        profile.setDisplayName("User");
        profile.setTimezone("UTC");
        profile.setRoleSummary("USER");
        profile.setAccountStatus(AccountStatus.ACTIVE);
        profile.setCreatedAt(OffsetDateTime.now());
        profile.setUpdatedAt(OffsetDateTime.now());
        return profile;
    }

    private NotificationPreferenceEntity preference(UUID userId) {
        NotificationPreferenceEntity preference = new NotificationPreferenceEntity();
        preference.setUserId(userId);
        preference.setInAppEnabled(true);
        preference.setEmailEnabled(true);
        preference.setReminderMinutesBefore(30);
        preference.setUpdatedAt(OffsetDateTime.now());
        return preference;
    }
}
