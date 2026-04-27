package com.teamresource.analytics.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourcePopularityRepository extends JpaRepository<ResourcePopularityEntity, UUID> {

    List<ResourcePopularityEntity> findAllByOrderByPopularityScoreDesc(Pageable pageable);
}
