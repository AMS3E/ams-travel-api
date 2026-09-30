package ams.asmtravel.api.services.point.service;

import ams.asmtravel.api.services.point.domain.PointSourceType;
import ams.asmtravel.api.services.point.dto.request.PointHistoryRequest;
import ams.asmtravel.api.services.point.dto.response.PointTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/** Points for ratings and reviews: one per person, place and day in the active season, whatever earned it. */
public interface PointService {

    /** Published after a point is recorded; badges listen once the change is committed. */
    record PointsAwardedEvent(UUID userId, UUID seasonId, int points) {
    }

    /** Counts a new rating or review and awards the day's point; returns 1 or 0. */
    int awardDailyTargetPoint(UUID userId, String targetType, UUID targetId, PointSourceType source, UUID sourceId);

    /** Points earned between two days, both inclusive. */
    long sumPoints(UUID userId, LocalDate from, LocalDate to);

    Page<PointTransactionResponse> findMine(UUID userId, PointHistoryRequest request, Pageable pageable);
}
