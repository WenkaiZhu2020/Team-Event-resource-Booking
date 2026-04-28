package com.teamresource.notification.infrastructure.persistence.repository;

import com.teamresource.notification.domain.model.NotificationChannel;
import com.teamresource.notification.domain.repository.NotificationTemplateRepository;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationTemplateRepositoryImpl implements NotificationTemplateRepository {

    private final SpringDataNotificationTemplateJpaRepository repository;

    public NotificationTemplateRepositoryImpl(SpringDataNotificationTemplateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<NotificationTemplateEntity> findActive(String templateCode, NotificationChannel channel) {
        return repository.findByTemplateCodeAndChannelAndActiveTrue(templateCode, channel);
    }
}
