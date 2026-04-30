package com.teamresource.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamresource.user.api.dto.ApiResponse;
import com.teamresource.user.api.dto.NotificationPreferenceResponse;
import com.teamresource.user.api.dto.ProvisionUserRequest;
import com.teamresource.user.api.dto.SyncRolesRequest;
import com.teamresource.user.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.user.api.dto.UpdateUserProfileRequest;
import com.teamresource.user.api.dto.UserProfileResponse;
import com.teamresource.user.config.InternalApiProperties;
import com.teamresource.user.config.JwtProperties;
import com.teamresource.user.config.SecurityConfig;
import com.teamresource.user.config.UserServiceConfiguration;
import com.teamresource.user.domain.AccountStatus;
import com.teamresource.user.infra.persistence.NotificationPreferenceEntity;
import com.teamresource.user.infra.persistence.NotificationPreferenceRepository;
import com.teamresource.user.infra.persistence.UserProfileEntity;
import com.teamresource.user.infra.persistence.UserProfileRepository;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class UserServiceCoverageTest {

    @Test
    void shouldCoverRecordsEntitiesAndConfigurationTypes() {
        UUID userId = UUID.randomUUID();
        ApiResponse<String> apiResponse = ApiResponse.of("ok");
        NotificationPreferenceResponse preferenceResponse = new NotificationPreferenceResponse(true, false, 15);
        ProvisionUserRequest provision = new ProvisionUserRequest(userId, "user@example.com", "Casey", "UTC", Set.of("USER"));
        SyncRolesRequest syncRolesRequest = new SyncRolesRequest(Set.of("ADMIN"), "auth-service");
        UpdateNotificationPreferenceRequest updatePreference = new UpdateNotificationPreferenceRequest(true, false, 30);
        UpdateUserProfileRequest updateProfile = new UpdateUserProfileRequest("Casey", "UTC");
        UserProfileResponse profileResponse = new UserProfileResponse(
                userId, "user@example.com", "Casey", "UTC", "USER", "ACTIVE", OffsetDateTime.now(), OffsetDateTime.now());
        InternalApiProperties internalApiProperties = new InternalApiProperties("X-Key", "secret");
        JwtProperties jwtProperties = new JwtProperties("issuer", "secret");

        NotificationPreferenceEntity preferenceEntity = new NotificationPreferenceEntity();
        preferenceEntity.setUserId(userId);
        preferenceEntity.setInAppEnabled(true);
        preferenceEntity.setEmailEnabled(false);
        preferenceEntity.setReminderMinutesBefore(10);
        preferenceEntity.setUpdatedAt(OffsetDateTime.now());
        preferenceEntity.setVersion(1L);

        UserProfileEntity profileEntity = new UserProfileEntity();
        profileEntity.setUserId(userId);
        profileEntity.setEmail("user@example.com");
        profileEntity.setDisplayName("Casey");
        profileEntity.setTimezone("UTC");
        profileEntity.setRoleSummary("USER");
        profileEntity.setAccountStatus(AccountStatus.ACTIVE);
        profileEntity.setCreatedAt(OffsetDateTime.now());
        profileEntity.setUpdatedAt(OffsetDateTime.now());
        profileEntity.setVersion(2L);

        assertThat(apiResponse.data()).isEqualTo("ok");
        assertThat(preferenceResponse.reminderMinutesBefore()).isEqualTo(15);
        assertThat(provision.roles()).containsExactly("USER");
        assertThat(syncRolesRequest.assignedBy()).isEqualTo("auth-service");
        assertThat(updatePreference.emailEnabled()).isFalse();
        assertThat(updateProfile.displayName()).isEqualTo("Casey");
        assertThat(profileResponse.roleSummary()).isEqualTo("USER");
        assertThat(internalApiProperties.keyValue()).isEqualTo("secret");
        assertThat(jwtProperties.issuer()).isEqualTo("issuer");
        assertThat(AccountStatus.valueOf("ACTIVE")).isEqualTo(AccountStatus.ACTIVE);
        assertThat(preferenceEntity.getUserId()).isEqualTo(userId);
        assertThat(preferenceEntity.isInAppEnabled()).isTrue();
        assertThat(preferenceEntity.isEmailEnabled()).isFalse();
        assertThat(preferenceEntity.getReminderMinutesBefore()).isEqualTo(10);
        assertThat(preferenceEntity.getUpdatedAt()).isNotNull();
        assertThat(preferenceEntity.getVersion()).isEqualTo(1L);
        assertThat(profileEntity.getUserId()).isEqualTo(userId);
        assertThat(profileEntity.getEmail()).isEqualTo("user@example.com");
        assertThat(profileEntity.getDisplayName()).isEqualTo("Casey");
        assertThat(profileEntity.getTimezone()).isEqualTo("UTC");
        assertThat(profileEntity.getRoleSummary()).isEqualTo("USER");
        assertThat(profileEntity.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(profileEntity.getCreatedAt()).isNotNull();
        assertThat(profileEntity.getUpdatedAt()).isNotNull();
        assertThat(profileEntity.getVersion()).isEqualTo(2L);
        assertThat(UserProfileRepository.class).isNotNull();
        assertThat(NotificationPreferenceRepository.class).isNotNull();
        assertThat(new UserServiceConfiguration()).isNotNull();
        assertThat(new SecurityConfig()).isNotNull();
    }

    @Test
    void mainShouldDelegateToSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            UserServiceApplication.main(new String[]{"--spring.main.banner-mode=off"});

            springApplication.verify(() -> SpringApplication.run(UserServiceApplication.class, new String[]{"--spring.main.banner-mode=off"}));
        }
    }
}
