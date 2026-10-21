package ams.asmtravel.api.services.activity.stamp.service.impl;

import ams.asmtravel.api.services.activity.stamp.config.StampProperties;
import ams.asmtravel.api.services.activity.stamp.domain.DigitalStamp;
import ams.asmtravel.api.services.activity.stamp.domain.StampCode;
import ams.asmtravel.api.services.activity.stamp.domain.StampCollectedEvent;
import ams.asmtravel.api.services.activity.stamp.domain.StampOutcome;
import ams.asmtravel.api.services.activity.stamp.domain.StampTarget;
import ams.asmtravel.api.services.activity.stamp.dto.request.StampCodeRequest;
import ams.asmtravel.api.services.activity.stamp.dto.request.StampRequest;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampCodeResponse;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampResponse;
import ams.asmtravel.api.services.activity.stamp.mapper.StampMapper;
import ams.asmtravel.api.services.activity.stamp.repository.StampCodeRepository;
import ams.asmtravel.api.services.activity.stamp.repository.StampRepository;
import ams.asmtravel.api.services.activity.stamp.service.StampService;
import ams.asmtravel.api.services.badge.dto.response.UserBadgeResponse;
import ams.asmtravel.api.services.badge.service.BadgeService;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Digital stamps. The scanned code names the place and the device position proves the visitor is there;
 * distance is plain haversine math, not a spatial query, and the position is trusted as sent.
 */
@Service
@RequiredArgsConstructor
public class StampServiceImpl implements StampService {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;
    /** Letters and digits a visitor can read back without doubt: no 0, O, 1 or I. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StampRepository stampRepository;
    private final StampCodeRepository codeRepository;
    private final StampMapper mapper;
    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final ActivityService activityService;
    private final SeasonService seasonService;
    private final BadgeService badgeService;
    private final StampProperties properties;
    private final ApplicationEventPublisher events;

    /** A destination of any kind as a check-in sees it. */
    private record Place(StampTarget type, UUID id, String name, boolean published, BigDecimal latitude, BigDecimal longitude) {
    }

    /**
     * A code nobody current holds answers INVALID_CODE; an unpublished place behind a current code answers 404 like a
     * missing one. Badge listeners run in this transaction; the answer lists what they awarded.
     */
    @Override
    @Transactional
    public StampResponse collect(UUID userId, StampRequest request) {
        Optional<StampCode> code = currentCode(request.code());
        if (code.isEmpty()) {
            return answer(null, null, null, StampOutcome.INVALID_CODE, null, List.of());
        }
        Place place = find(code.get().getTargetType(), code.get().getTargetId())
                .filter(Place::published)
                .orElseThrow(() -> new DestinationNotFoundException());
        if (place.latitude() == null || place.longitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This destination has no location to check in against");
        }

        int distance = (int) Math.round(distanceMeters(request.latitude(), request.longitude(),
                place.latitude().doubleValue(), place.longitude().doubleValue()));
        if (distance > properties.stampRadiusMeters()) {
            return answer(null, place.type(), place.id(), StampOutcome.TOO_FAR, distance, List.of());
        }
        SeasonService.Window season = seasonWindow();
        if (stampRepository.existsByUserIdAndDestinationIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                userId, place.id(), season.from(), season.until())) {
            return answer(null, place.type(), place.id(), StampOutcome.ALREADY_COLLECTED, distance, List.of());
        }
        Instant since = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        DigitalStamp stamp = stampRepository.saveAndFlush(new DigitalStamp(userId, place.type(), place.id()));
        events.publishEvent(new StampCollectedEvent(userId, place.type(), place.id(), stamp.getId()));
        return answer(stamp, place.type(), place.id(), StampOutcome.COLLECTED, distance, badgeService.findEarnedSince(userId, since));
    }

    @Override
    @Transactional(readOnly = true)
    public StampResponse alreadyCollected(StampRequest request) {
        StampCode code = currentCode(request.code()).orElse(null);
        return answer(null, code == null ? null : code.getTargetType(), code == null ? null : code.getTargetId(),
                code == null ? StampOutcome.INVALID_CODE : StampOutcome.ALREADY_COLLECTED, null, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StampResponse> findMine(UUID userId) {
        return stampRepository.findByUserIdOrderByCollectedAtDesc(userId).stream().map(mapper::toResponse).toList();
    }

    // Codes

    @Override
    @Transactional(readOnly = true)
    public List<StampCodeResponse> findCodes(StampTarget targetType, UUID targetId) {
        return codeRepository.findByTargetTypeAndTargetIdOrderByIssuedAtDesc(targetType, targetId).stream()
                .map(this::respond)
                .toList();
    }

    /** The place may still be a draft: a code can be printed before the listing goes live. */
    @Override
    @Transactional
    public StampCodeResponse issueCode(StampCodeRequest request, UUID issuedBy) {
        if (find(request.targetType(), request.targetId()).isEmpty()) {
            throw new DestinationNotFoundException();
        }
        Instant now = Instant.now();
        codeRepository.findByTargetTypeAndTargetIdAndReplacedAtIsNull(request.targetType(), request.targetId())
                .ifPresent(previous -> previous.setReplacedAt(now));
        String code = newCode();
        while (codeRepository.findByCode(code).isPresent()) {
            code = newCode();
        }
        return respond(codeRepository.saveAndFlush(new StampCode(request.targetType(), request.targetId(), code, issuedBy)));
    }

    @Override
    @Transactional
    public void withdrawCode(UUID codeId) {
        StampCode code = codeRepository.findById(codeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such check-in code"));
        codeRepository.delete(code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> placeNameOf(String code) {
        return currentCode(code).flatMap(current -> find(current.getTargetType(), current.getTargetId())).map(Place::name);
    }

    // For other services

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

    // Helpers

    /** The current code the scanned text names: the code itself, or the scan link whose last part it is. */
    private Optional<StampCode> currentCode(String scanned) {
        if (scanned == null || scanned.isBlank()) {
            return Optional.empty();
        }
        String text = scanned.trim();
        int slash = text.lastIndexOf('/');
        String code = (slash >= 0 ? text.substring(slash + 1) : text).trim().toUpperCase(Locale.ROOT);
        return codeRepository.findByCode(code).filter(StampCode::current);
    }

    /** The season's window when one is active; all time otherwise, so a place is never stamped twice between seasons. */
    private SeasonService.Window seasonWindow() {
        Optional<SeasonResponse> season = seasonService.findCurrent();
        return season.map(current -> seasonService.windowOf(current.startsOn(), current.endsOn()))
                .orElseGet(() -> new SeasonService.Window(Instant.EPOCH, Instant.now().plus(1, ChronoUnit.DAYS)));
    }

    private Optional<Place> find(StampTarget type, UUID id) {
        return switch (type) {
            case DESTINATION -> destinationService.findSummary(id)
                    .map(place -> new Place(type, place.id(), place.name(), place.published(), place.latitude(), place.longitude()));
            case STAY -> stayService.exists(id) ? Optional.of(stayService.findById(id)).map(stay -> new Place(type, stay.id(),
                    stay.name(), stay.status() == ListingStatus.PUBLISHED, stay.latitude(), stay.longitude())) : Optional.empty();
            case FOOD -> foodService.exists(id) ? Optional.of(foodService.findById(id)).map(food -> new Place(type, food.id(),
                    food.name(), food.status() == ListingStatus.PUBLISHED, food.latitude(), food.longitude())) : Optional.empty();
            case ACTIVITY -> activityService.exists(id) ? Optional.of(activityService.findById(id)).map(activity -> new Place(type,
                    activity.id(), activity.name(), activity.status() == ListingStatus.PUBLISHED, activity.latitude(), activity.longitude()))
                    : Optional.empty();
        };
    }

    private StampCodeResponse respond(StampCode code) {
        return mapper.toResponse(code, properties.scanUrl(code.getCode()));
    }

    private StampResponse answer(DigitalStamp stamp, StampTarget type, UUID destinationId, StampOutcome outcome,
                                 Integer distance, List<UserBadgeResponse> earned) {
        return new StampResponse(
                stamp == null ? null : stamp.getId(),
                type,
                destinationId,
                stamp == null ? null : stamp.getCollectedAt(),
                stamp == null ? null : stamp.getSource(),
                outcome, distance, properties.stampRadiusMeters(), earned);
    }

    /** Three groups of four, like ABCD-EFGH-JKLM: easy to read off a print. */
    static String newCode() {
        StringBuilder code = new StringBuilder(14);
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                code.append('-');
            }
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
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
