package com.teamresource.notification.application.service;

import com.teamresource.notification.api.dto.UpdateNotificationPreferenceRequest;
import com.teamresource.notification.domain.repository.NotificationPreferenceRepository;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationPreferenceEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional
    public NotificationPreferenceEntity getOrCreate(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
    }

    @Transactional
    public NotificationPreferenceEntity update(UUID userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreferenceEntity preference = getOrCreate(userId);
        preference.setInAppEnabled(request.inAppEnabled());
        preference.setEmailEnabled(request.emailEnabled());
        preference.setReminderLeadMinutes(request.reminderLeadMinutes());
        preference.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return preferenceRepository.save(preference);
    }

    private NotificationPreferenceEntity createDefault(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        NotificationPreferenceEntity pref = new NotificationPreferenceEntity();
        pref.setUserId(userId);
        pref.setInAppEnabled(true);
        pref.setEmailEnabled(true);
        pref.setReminderLeadMinutes(60);
        pref.setCreatedAt(now);
        pref.setUpdatedAt(now);
        return preferenceRepository.save(pref);
    }
}
