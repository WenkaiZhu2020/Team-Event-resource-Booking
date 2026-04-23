package com.teamresource.user.service;

import com.teamresource.user.api.dto.NotificationPreferenceResponse;
import com.teamresource.user.api.dto.ProvisionUserRequest;
import com.teamresource.user.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.user.api.dto.UpdateUserProfileRequest;
import com.teamresource.user.api.dto.UserProfileResponse;
import com.teamresource.user.domain.AccountStatus;
import com.teamresource.user.infra.persistence.NotificationPreferenceEntity;
import com.teamresource.user.infra.persistence.NotificationPreferenceRepository;
import com.teamresource.user.infra.persistence.UserProfileEntity;
import com.teamresource.user.infra.persistence.UserProfileRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public UserProfileService(
            UserProfileRepository userProfileRepository,
            NotificationPreferenceRepository notificationPreferenceRepository
    ) {
        this.userProfileRepository = userProfileRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Transactional
    public UserProfileResponse provision(ProvisionUserRequest request) {
        if (userProfileRepository.existsById(request.userId())) {
            return toResponse(userProfileRepository.findById(request.userId()).orElseThrow());
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(request.userId());
        profile.setEmail(normalizeEmail(request.email()));
        profile.setDisplayName(request.displayName().trim());
        profile.setTimezone(request.timezone().trim());
        profile.setRoleSummary(buildRoleSummary(request.roles()));
        profile.setAccountStatus(AccountStatus.ACTIVE);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        userProfileRepository.save(profile);

        NotificationPreferenceEntity preference = new NotificationPreferenceEntity();
        preference.setUserId(request.userId());
        preference.setInAppEnabled(true);
        preference.setEmailEnabled(true);
        preference.setReminderMinutesBefore(30);
        preference.setUpdatedAt(now);
        notificationPreferenceRepository.save(preference);

        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse me(UUID userId) {
        return toResponse(findProfile(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        UserProfileEntity profile = findProfile(userId);
        profile.setDisplayName(request.displayName().trim());
        profile.setTimezone(request.timezone().trim());
        profile.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toResponse(userProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse preferences(UUID userId) {
        return toPreferenceResponse(findPreference(userId));
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreferenceEntity preference = findPreference(userId);
        preference.setInAppEnabled(request.inAppEnabled());
        preference.setEmailEnabled(request.emailEnabled());
        preference.setReminderMinutesBefore(request.reminderMinutesBefore());
        preference.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toPreferenceResponse(notificationPreferenceRepository.save(preference));
    }

    private UserProfileEntity findProfile(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));
    }

    private NotificationPreferenceEntity findPreference(UUID userId) {
        return notificationPreferenceRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification preference not found"));
    }

    private UserProfileResponse toResponse(UserProfileEntity profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getTimezone(),
                profile.getRoleSummary(),
                profile.getAccountStatus().name(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private NotificationPreferenceResponse toPreferenceResponse(NotificationPreferenceEntity preference) {
        return new NotificationPreferenceResponse(
                preference.isInAppEnabled(),
                preference.isEmailEnabled(),
                preference.getReminderMinutesBefore()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildRoleSummary(java.util.Set<String> roles) {
        return roles.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + "," + right)
                .orElse("USER");
    }
}
