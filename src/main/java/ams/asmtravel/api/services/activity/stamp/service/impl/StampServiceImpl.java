package ams.asmtravel.api.services.activity.stamp.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;

import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.services.activity.stamp.config.StampProperties;
import ams.asmtravel.api.services.activity.stamp.domain.DigitalStamp;
import ams.asmtravel.api.services.activity.stamp.domain.StampCollectedEvent;
import ams.asmtravel.api.services.activity.stamp.domain.StampOutcome;
import ams.asmtravel.api.services.activity.stamp.dto.request.StampRequest;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampResponse;
import ams.asmtravel.api.services.activity.stamp.mapper.StampMapper;
import ams.asmtravel.api.services.activity.stamp.repository.StampRepository;
import ams.asmtravel.api.services.activity.stamp.service.StampService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.badge.dto.response.UserBadgeResponse;
import ams.asmtravel.api.services.badge.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Digital stamps. Distance is plain haversine math, not a spatial query; the device position is trusted as sent. */
@Service
@RequiredArgsConstructor
public class StampServiceImpl implements StampService {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private final StampRepository stampRepository;
    private final StampMapper mapper;
    private final DestinationService destinationService;
    private final SeasonService seasonService;
    private final BadgeService badgeService;
    private final StampProperties properties;
    private final ApplicationEventPublisher events;

    /** Drafts and unknown ids both get 404. Badge listeners run in this transaction; the answer lists what they awarded. */
    @Override
    @Transactional
    public StampResponse collect(UUID userId, StampRequest request) {
        UUID destinationId = request.destinationId();
        DestinationSummary place = destinationService.findSummary(destinationId)
                .filter(DestinationSummary::published)
                .orElseThrow(() -> new DestinationNotFoundException());
        if (place.latitude() == null || place.longitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This destination has no location to check in against");
        }

        int distance = (int) Math.round(distanceMeters(request.latitude(), request.longitude(),
                place.latitude().doubleValue(), place.longitude().doubleValue()));
        if (distance > properties.stampRadiusMeters()) {
            return answer(null, destinationId, StampOutcome.TOO_FAR, distance, List.of());
        }
        LocalDate today = seasonService.today();
        SeasonService.Window day = seasonService.windowOf(today, today);
        if (stampRepository.existsByUserIdAndDestinationIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                userId, destinationId, day.from(), day.until())) {
            return answer(null, destinationId, StampOutcome.ALREADY_COLLECTED, distance, List.of());
        }
        Instant since = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DigitalStamp stamp = stampRepository.saveAndFlush(new DigitalStamp(userId, destinationId));
        events.publishEvent(new StampCollectedEvent(userId, destinationId, stamp.getId()));
        return answer(stamp, destinationId, StampOutcome.COLLECTED, distance, badgeService.findEarnedSince(userId, since));
    }

    @Override
    public StampResponse alreadyCollected(UUID destinationId) {
        return answer(null, destinationId, StampOutcome.ALREADY_COLLECTED, 0, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StampResponse> findMine(UUID userId) {
        return stampRepository.findByUserIdOrderByCollectedAtDesc(userId).stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countBetween(UUID userId, Instant from, Instant until) {
        return stampRepository.countByUserIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(userId, from, until);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPlacesStamped(UUID userId, Collection<UUID> destinationIds, Instant from, Instant until) {
        return destinationIds == null || destinationIds.isEmpty()
                ? 0
                : stampRepository.countPlacesStamped(userId, destinationIds, from, until);
    }

    /** A person's stamps go with them. Runs in the delete's own transaction. */
    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        stampRepository.deleteByUserId(event.userId());
    }

    private StampResponse answer(DigitalStamp stamp, UUID destinationId, StampOutcome outcome, int distance,
                                 List<UserBadgeResponse> earned) {
        return new StampResponse(
                stamp == null ? null : stamp.getId(),
                destinationId,
                stamp == null ? null : stamp.getCollectedAt(),
                stamp == null ? null : stamp.getSource(),
                outcome, distance, properties.stampRadiusMeters(), earned);
    }

    /** Great-circle distance by the haversine formula. */
    static double distanceMeters(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        double deltaLatitude = Math.toRadians(toLatitude - fromLatitude);
        double deltaLongitude = Math.toRadians(toLongitude - fromLongitude);
        double a = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(Math.toRadians(fromLatitude)) * Math.cos(Math.toRadians(toLatitude))
                * Math.pow(Math.sin(deltaLongitude / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
