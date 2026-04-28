package com.teamresource.booking.application.specification;

import com.teamresource.booking.domain.model.BookingSearchCriteria;
import com.teamresource.booking.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.domain.Specification;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<BookingEntity> fromCriteria(BookingSearchCriteria criteria) {
        Specification<BookingEntity> specification = Specification.where(null);
        if (criteria == null) {
            return specification;
        }

        if (criteria.userId() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("userId"), criteria.userId()));
        }
        if (criteria.resourceId() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("resourceId"), criteria.resourceId()));
        }
        if (criteria.status() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.from() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), criteria.from()));
        }
        if (criteria.to() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startAt"), criteria.to()));
        }

        return specification;
    }
}
