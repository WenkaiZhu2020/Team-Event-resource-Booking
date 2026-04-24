package com.teamresource.resource.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ResourceRepository extends JpaRepository<ResourceEntity, UUID>, JpaSpecificationExecutor<ResourceEntity> {

    @EntityGraph(attributePaths = {"availabilityRules", "maintenanceSlots"})
    List<ResourceEntity> findByManagerIdOrderByCreatedAtDesc(UUID managerId);
}
