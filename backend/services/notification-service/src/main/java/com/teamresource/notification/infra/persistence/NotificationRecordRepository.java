package com.teamresource.notification.infra.persistence;

import com.teamresource.notification.domain.NotificationChannel;
import com.teamresource.notification.domain.NotificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecordEntity, UUID> {

    List<NotificationRecordEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<NotificationRecordEntity> findByUserIdAndChannelOrderByCreatedAtDesc(UUID userId, NotificationChannel channel);

    long countByUserIdAndStatusNot(UUID userId, NotificationStatus status);
}
