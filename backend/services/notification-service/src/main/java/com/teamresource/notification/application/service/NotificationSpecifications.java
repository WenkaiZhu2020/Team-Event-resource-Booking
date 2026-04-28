package com.teamresource.notification.application.service;

import com.teamresource.notification.domain.model.NotificationSearchCriteria;
import com.teamresource.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationSpecifications {

    private NotificationSpecifications() {
    }

    public static Specification<NotificationEntity> from(NotificationSearchCriteria criteria) {
        Specification<NotificationEntity> spec = Specification.where(null);

        if (criteria.userId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), criteria.userId()));
        }
        if (criteria.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.channel() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("channel"), criteria.channel()));
        }
        if (criteria.type() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), criteria.type()));
        }
        if (Boolean.TRUE.equals(criteria.unreadOnly())) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("readAt")));
        }

        return spec;
    }
}
