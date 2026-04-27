package com.teamresource.analytics.service;

import com.teamresource.analytics.infra.persistence.BookingFactRepository;
import com.teamresource.analytics.infra.persistence.ResourcePopularityEntity;
import com.teamresource.analytics.infra.persistence.ResourcePopularityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourcePopularityRefreshService {

    private final BookingFactRepository bookingFactRepository;
    private final ResourcePopularityRepository resourcePopularityRepository;

    public ResourcePopularityRefreshService(
            BookingFactRepository bookingFactRepository,
            ResourcePopularityRepository resourcePopularityRepository
    ) {
        this.bookingFactRepository = bookingFactRepository;
        this.resourcePopularityRepository = resourcePopularityRepository;
    }

    @Scheduled(fixedDelayString = "${app.analytics.popularity-refresh-ms:300000}")
    @Transactional
    public void refresh() {
        List<BookingFactRepository.ResourcePopularityProjection> projections = bookingFactRepository.summarizeResourcePopularity();
        resourcePopularityRepository.deleteAllInBatch();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<ResourcePopularityEntity> entities = projections.stream().map(projection -> {
            ResourcePopularityEntity entity = new ResourcePopularityEntity();
            entity.setResourceId(projection.getResourceId());
            entity.setResourceName(projection.getResourceName());
            entity.setResourceType(projection.getResourceType());
            entity.setTotalBookings(projection.getTotalBookings());
            entity.setApprovedBookings(projection.getApprovedBookings());
            entity.setPendingBookings(projection.getPendingBookings());
            entity.setWaitlistedBookings(projection.getWaitlistedBookings());
            entity.setCancelledBookings(projection.getCancelledBookings());
            entity.setTotalReservedMinutes(projection.getTotalReservedMinutes());
            entity.setPopularityScore(score(projection));
            entity.setLastRefreshedAt(now);
            return entity;
        }).toList();
        resourcePopularityRepository.saveAll(entities);
    }

    private BigDecimal score(BookingFactRepository.ResourcePopularityProjection projection) {
        double raw = projection.getApprovedBookings() * 3.0
                + projection.getPendingBookings() * 1.5
                + projection.getWaitlistedBookings() * 1.0
                + (projection.getTotalReservedMinutes() / 60.0);
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
    }
}
