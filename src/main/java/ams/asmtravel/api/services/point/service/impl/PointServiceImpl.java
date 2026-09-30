package ams.asmtravel.api.services.point.service.impl;

import ams.asmtravel.api.services.point.domain.PointSourceType;
import ams.asmtravel.api.services.point.dto.request.PointHistoryRequest;
import ams.asmtravel.api.services.point.dto.response.PointTransactionResponse;
import ams.asmtravel.api.services.point.mapper.PointMapper;
import ams.asmtravel.api.services.point.repository.PointActivityRepository;
import ams.asmtravel.api.services.point.repository.PointTransactionRepository;
import ams.asmtravel.api.services.point.service.PointService;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private static final int DAILY_TARGET_POINT = 1;

    private final PointTransactionRepository pointRepository;
    private final PointActivityRepository activityRepository;
    private final PointMapper mapper;
    private final SeasonService seasonService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public int awardDailyTargetPoint(UUID userId, String targetType, UUID targetId, PointSourceType source, UUID sourceId) {
        Optional<SeasonResponse> season = seasonService.findCurrent();
        if (season.isEmpty()) {
            log.debug("No active season: {} {} earns no point", source, sourceId);
            return 0;
        }
        UUID seasonId = season.get().id();
        int awarded = pointRepository.insertIfAbsent(UUID.randomUUID(), userId, targetType, targetId, source.name(),
                sourceId, seasonId, DAILY_TARGET_POINT, seasonService.today(), Instant.now()) == 1 ? DAILY_TARGET_POINT : 0;
        boolean newTarget = activityRepository.insertIfAbsent(UUID.randomUUID(), userId, seasonId, source.name(),
                targetType, targetId, Instant.now()) == 1;
        boolean rating = source == PointSourceType.RATING;
        seasonService.addProgress(userId, seasonId, awarded, rating ? 1 : 0, rating ? 0 : 1,
                rating && newTarget ? 1 : 0, !rating && newTarget ? 1 : 0);
        if (awarded > 0) {
            events.publishEvent(new PointService.PointsAwardedEvent(userId, seasonId, awarded));
        }
        return awarded;
    }

    @Override
    @Transactional(readOnly = true)
    public long sumPoints(UUID userId, LocalDate from, LocalDate to) {
        return pointRepository.sumPoints(userId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PointTransactionResponse> findMine(UUID userId, PointHistoryRequest request, Pageable pageable) {
        return (request.seasonId() == null
                ? pointRepository.findByUserId(userId, pageable)
                : pointRepository.findByUserIdAndSeasonId(userId, request.seasonId(), pageable))
                .map(mapper::toResponse);
    }

    /** Points are personal data: they go with the person. */
    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        activityRepository.deleteByUser(event.userId());
        pointRepository.deleteByUser(event.userId());
    }
}
