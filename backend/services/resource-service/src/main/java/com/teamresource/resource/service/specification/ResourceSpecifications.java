package com.teamresource.resource.service.specification;

import com.teamresource.resource.domain.ApprovalMode;
import com.teamresource.resource.domain.ResourceStatus;
import com.teamresource.resource.domain.ResourceType;
import com.teamresource.resource.infra.persistence.ResourceEntity;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ResourceSpecifications {

    private ResourceSpecifications() {
    }

    public static Specification<ResourceEntity> visibleCatalog(
            String query,
            ResourceType type,
            ResourceStatus status,
            Boolean requiresApproval
    ) {
        return Specification.where(nameOrLocationContains(query))
                .and(hasType(type))
                .and(hasStatus(status))
                .and(hasApprovalRequirement(requiresApproval));
    }

    private static Specification<ResourceEntity> nameOrLocationContains(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, ignoredQuery, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("location")), like)
        );
    }

    private static Specification<ResourceEntity> hasType(ResourceType type) {
        if (type == null) {
            return null;
        }
        return (root, ignoredQuery, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<ResourceEntity> hasStatus(ResourceStatus status) {
        if (status == null) {
            return null;
        }
        return (root, ignoredQuery, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<ResourceEntity> hasApprovalRequirement(Boolean requiresApproval) {
        if (requiresApproval == null) {
            return null;
        }
        return requiresApproval
                ? (root, ignoredQuery, cb) -> cb.notEqual(root.get("approvalMode"), ApprovalMode.AUTO_APPROVE)
                : (root, ignoredQuery, cb) -> cb.equal(root.get("approvalMode"), ApprovalMode.AUTO_APPROVE);
    }
}
