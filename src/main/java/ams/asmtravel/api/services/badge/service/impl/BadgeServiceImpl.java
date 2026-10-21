package ams.asmtravel.api.services.badge.service.impl;

import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import ams.asmtravel.api.services.activity.popular.service.PopularService;
import ams.asmtravel.api.services.activity.stamp.domain.StampCollectedEvent;
import ams.asmtravel.api.services.activity.stamp.service.StampService;
import ams.asmtravel.api.services.badge.config.BadgeProperties;
import ams.asmtravel.api.services.badge.domain.Badge;
import ams.asmtravel.api.services.badge.domain.BadgeLevel;
import ams.asmtravel.api.services.badge.domain.BadgeStatus;
import ams.asmtravel.api.services.badge.domain.BadgeType;
import ams.asmtravel.api.services.badge.domain.UserBadge;
import ams.asmtravel.api.services.badge.dto.request.CreateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.request.UpdateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.response.BadgeProgressResponse;
import ams.asmtravel.api.services.badge.dto.response.BadgeResponse;
import ams.asmtravel.api.services.badge.dto.response.UserBadgeResponse;
import ams.asmtravel.api.services.badge.exception.BadgeNotFoundException;
import ams.asmtravel.api.services.badge.exception.InvalidBadgeException;
import ams.asmtravel.api.services.badge.mapper.BadgeMapper;
import ams.asmtravel.api.services.badge.repository.BadgeRepository;
import ams.asmtravel.api.services.badge.repository.UserBadgeRepository;
import ams.asmtravel.api.services.badge.service.BadgeService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.review.review.service.ReviewService;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeMapper mapper;
    private final SeasonService seasonService;
    private final ReviewService reviewService;
    private final PopularService popularService;
    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final UserService userService;
    // A provider: the stamp service asks this one what a stamp earned.
    private final ObjectProvider<StampService> stampService;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate newTransaction;
    private final BadgeProperties properties;

    public BadgeServiceImpl(BadgeRepository badgeRepository, UserBadgeRepository userBadgeRepository, BadgeMapper mapper,
                            SeasonService seasonService, ReviewService reviewService,
                            PopularService popularService, DestinationService destinationService,
                            StayService stayService, FoodService foodService, UserService userService,
                            ObjectProvider<StampService> stampService, ApplicationEventPublisher events,
                            PlatformTransactionManager transactionManager, BadgeProperties properties) {
        this.properties = properties;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.mapper = mapper;
        this.seasonService = seasonService;
        this.reviewService = reviewService;
        this.popularService = popularService;
        this.destinationService = destinationService;
        this.stayService = stayService;
        this.foodService = foodService;
        this.userService = userService;
        this.stampService = stampService;
        this.events = events;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // Definitions

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getAll(BadgeType type, BadgeStatus status) {
        return badgeRepository.findAllByOrderByTypeAscThresholdAsc().stream()
                .filter(badge -> type == null || badge.getType() == type)
                .filter(badge -> status == null || badge.getStatus() == status)
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BadgeResponse findById(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public BadgeResponse create(CreateBadgeRequest request) {
        check(request.code(), request.type(), request.level(), request.destinationIds(), null);
        Badge badge = mapper.toBadge(request);
        badge.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        badge.setStatus(request.status() == null ? BadgeStatus.ACTIVE : request.status());
        badge.setDestinationIds(destinationsOf(request.type(), request.destinationIds()));
        return mapper.toResponse(badgeRepository.saveAndFlush(badge));
    }

    @Override
    @Transactional
    public BadgeResponse update(UUID id, UpdateBadgeRequest request) {
        Badge badge = find(id);
        check(request.code(), request.type(), request.level(), request.destinationIds(), id);
        mapper.apply(request, badge);
        badge.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        if (request.status() != null) {
            badge.setStatus(request.status());
        }
        badge.setDestinationIds(destinationsOf(request.type(), request.destinationIds()));
        return mapper.toResponse(badgeRepository.saveAndFlush(badge));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Badge badge = find(id);
        if (userBadgeRepository.existsByBadgeIdAndEarnedAtIsNotNull(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "People have earned this badge; set it INACTIVE instead so their history stays");
        }
        userBadgeRepository.deleteByBadge(id);
        badgeRepository.delete(badge);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return badgeRepository.count();
    }

    // A person's badges

    @Override
    @Transactional(readOnly = true)
    public BadgeProgressResponse findProgress(UUID userId, UUID seasonId) {
        SeasonResponse season = seasonId == null ? seasonService.findCurrent().orElse(null) : seasonService.findById(seasonId);
        return respond(userId, season, List.of(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeResponse> findEarned(UUID userId) {
        return respond(userBadgeRepository.findByUserIdAndEarnedAtIsNotNullOrderByEarnedAtDesc(userId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public UserBadgeResponse findShared(UUID userBadgeId) {
        return userBadgeRepository.findById(userBadgeId)
                .filter(userBadge -> userBadge.getEarnedAt() != null)
                .map(userBadge -> respond(List.of(userBadge), true).getFirst())
                .orElseThrow(BadgeNotFoundException::new);
    }

    /** Checked on every open, not just the first: the visit may already be counted. Drafts answer like missing ids. */
    @Override
    @Transactional
    public BadgeProgressResponse explore(UUID userId, UUID destinationId) {
        BrowseTarget kind = destinationService.isPublished(destinationId) ? BrowseTarget.DESTINATION
                : stayService.isPublished(destinationId) ? BrowseTarget.STAY
                : foodService.isPublished(destinationId) ? BrowseTarget.FOOD
                : null;
        if (kind == null) {
            throw new DestinationNotFoundException();
        }
        boolean counted = popularService.recordBrowse(userId, kind, destinationId);
        List<UserBadgeResponse> earned = evaluate(userId, EnumSet.of(BadgeType.BROWSES));
        return respond(userId, seasonService.findCurrent().orElse(null), earned, counted);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeResponse> findEarnedSince(UUID userId, Instant since) {
        return respond(userBadgeRepository.findByUserIdAndEarnedAtGreaterThanEqualOrderByEarnedAtAsc(userId, since), false);
    }

    // What other services report

    /** Runs in the stamp's transaction, so the count includes it and the stamp answer lists what it earned. */
    @EventListener
    public void onStampCollected(StampCollectedEvent event) {
        evaluate(event.userId(), EnumSet.of(BadgeType.STAMPS, BadgeType.DESTINATION_STAMPS));
    }

    /** After the review commits, in its own transaction, so a failure never undoes the review. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewPublished(ReviewService.ReviewPublishedEvent event) {
        evaluateAfterCommit(event.userId(), BadgeType.REVIEWS);
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        userBadgeRepository.deleteByUser(event.userId());
    }

    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        badgeRepository.removeDestination(event.destinationId());
    }

    private void evaluateAfterCommit(UUID userId, BadgeType type) {
        try {
            newTransaction.executeWithoutResult(status -> evaluate(userId, EnumSet.of(type)));
        } catch (RuntimeException failure) {
            // The next activity retries it: unlocking skips what is already held.
            log.warn("Could not update {} badges for user {}", type, userId, failure);
        }
    }

    /** Re-counts these types for the active season and unlocks what was reached. */
    private List<UserBadgeResponse> evaluate(UUID userId, Set<BadgeType> types) {
        Optional<SeasonResponse> season = seasonService.findCurrent();
        List<Badge> badges = badgeRepository.findByStatusAndTypeInOrderByThresholdAsc(BadgeStatus.ACTIVE, types);
        if (season.isEmpty() || badges.isEmpty()) {
            return List.of();
        }
        SeasonService.Window window = seasonService.windowOf(season.get().startsOn(), season.get().endsOn());
        // A ladder's badges share one count, so each type is counted once.
        Map<BadgeType, Long> counted = new EnumMap<>(BadgeType.class);
        Instant now = Instant.now();
        List<UserBadge> unlocked = new ArrayList<>();
        for (Badge badge : badges) {
            long count = badge.getType() == BadgeType.DESTINATION_STAMPS
                    ? count(userId, badge, season.get(), window)
                    : counted.computeIfAbsent(badge.getType(), type -> count(userId, badge, season.get(), window));
            UserBadge userBadge = userBadgeRepository.findByUserIdAndBadgeIdAndSeasonId(userId, badge.getId(), season.get().id())
                    .orElseGet(() -> new UserBadge(userId, badge, season.get().id()));
            userBadge.setProgress((int) Math.min(count, Integer.MAX_VALUE));
            userBadge.setUpdatedAt(now);
            if (userBadge.getEarnedAt() == null && count >= badge.getThreshold()) {
                userBadge.setEarnedAt(now);
                unlocked.add(userBadge);
            }
            userBadgeRepository.save(userBadge);
        }
        userBadgeRepository.flush();
        for (UserBadge userBadge : unlocked) {
            events.publishEvent(new BadgeEarnedEvent(userId, userBadge.getId(), userBadge.getBadge().getName(), season.get().name()));
            log.info("User {} earned {} in {}", userId, userBadge.getBadge().getCode(), season.get().name());
        }
        return respond(unlocked, false);
    }

    /** A person's count of what a badge counts, inside one season. */
    private long count(UUID userId, Badge badge, SeasonResponse season, SeasonService.Window window) {
        return switch (badge.getType()) {
            case BROWSES -> popularService.countDailyBrowses(userId, window.from(), window.until());
            case REVIEWS -> reviewService.countPublishedByUserBetween(userId, window.from(), window.until());
            case STAMPS -> stampService.getObject().countBetween(userId, window.from(), window.until());
            case DESTINATION_STAMPS -> stampService.getObject().countPlacesStamped(userId,
                    Set.copyOf(badge.getDestinationIds()), window.from(), window.until());
        };
    }

    private BadgeProgressResponse respond(UUID userId, SeasonResponse season, List<UserBadgeResponse> earned,
                                          Boolean browseCounted) {
        if (season == null) {
            return new BadgeProgressResponse(null, null, List.of(), List.of(), earned, browseCounted, null);
        }
        List<UserBadge> rows = userBadgeRepository.findByUserIdAndSeasonIdOrderByUpdatedAtDesc(userId, season.id());
        List<Badge> ladders = badgeRepository.findByStatusAndTypeInOrderByThresholdAsc(BadgeStatus.ACTIVE,
                Arrays.stream(BadgeType.values()).filter(BadgeType::hasLevels).toList());
        Map<UUID, UserBadge> byBadge = rows.stream().collect(Collectors.toMap(row -> row.getBadge().getId(), Function.identity()));
        List<BadgeProgressResponse.Track> tracks = Arrays.stream(BadgeType.values())
                .filter(BadgeType::hasLevels)
                .map(type -> track(type, ladders, byBadge))
                .toList();
        return new BadgeProgressResponse(season.id(), season.name(), tracks, respond(rows, false), earned, browseCounted,
                percentile(userId, season.id()));
    }

    /**
     * Where a person stands among the season's active members, as "top N%": members ranked by badges
     * earned, then by progress added up over every ladder; the person's rank over the active count,
     * rounded up. Null for someone with no activity yet, and no rank to speak of.
     */
    private Integer percentile(UUID userId, UUID seasonId) {
        record Score(UUID userId, long earned, long progress) {
        }
        List<Score> active = userBadgeRepository.activityIn(seasonId).stream()
                .map(row -> new Score((UUID) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .filter(score -> score.earned() > 0 || score.progress() > 0)
                .toList();
        Score mine = active.stream().filter(score -> score.userId().equals(userId)).findFirst().orElse(null);
        if (mine == null) {
            return null;
        }
        long ahead = active.stream()
                .filter(other -> other.earned() > mine.earned() || (other.earned() == mine.earned() && other.progress() > mine.progress()))
                .count();
        return (int) Math.max(1, Math.ceil(100.0 * (ahead + 1) / active.size()));
    }

    /** One ladder: the stored progress, the highest level unlocked, and what the next one takes. */
    private static BadgeProgressResponse.Track track(BadgeType type, List<Badge> ladders, Map<UUID, UserBadge> byBadge) {
        Map<BadgeLevel, Integer> thresholds = new EnumMap<>(BadgeLevel.class);
        long count = 0;
        BadgeLevel level = null;
        for (Badge badge : ladders) {
            if (badge.getType() != type || badge.getLevel() == null) {
                continue;
            }
            thresholds.putIfAbsent(badge.getLevel(), badge.getThreshold());
            UserBadge row = byBadge.get(badge.getId());
            if (row != null) {
                count = Math.max(count, row.getProgress());
                if (row.getEarnedAt() != null && (level == null || badge.getLevel().ordinal() > level.ordinal())) {
                    level = badge.getLevel();
                }
            }
        }
        BadgeLevel held = level;
        BadgeLevel next = thresholds.keySet().stream()
                .filter(candidate -> held == null || candidate.ordinal() > held.ordinal())
                .findFirst().orElse(null);
        Long remaining = next == null ? null : Math.max(0, thresholds.get(next) - count);
        return new BadgeProgressResponse.Track(type, count, held, next, remaining, thresholds);
    }

    /** Season names for every row, and holder names only for a share page. */
    private List<UserBadgeResponse> respond(List<UserBadge> rows, boolean withHolder) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> seasons = new HashMap<>();
        for (UUID seasonId : rows.stream().map(UserBadge::getSeasonId).collect(Collectors.toSet())) {
            seasons.put(seasonId, seasonService.findById(seasonId).name());
        }
        Map<UUID, UserSummary> holders = withHolder
                ? userService.findSummaries(rows.stream().map(UserBadge::getUserId).distinct().toList())
                : Map.of();
        return rows.stream()
                .map(row -> mapper.toUserBadgeResponse(row, seasons.get(row.getSeasonId()),
                        withHolder && holders.containsKey(row.getUserId()) ? holders.get(row.getUserId()).fullName() : null,
                        row.getEarnedAt() == null ? null : properties.shareUrl(row.getId())))
                .toList();
    }

    private void check(String code, BadgeType type, BadgeLevel level, Collection<UUID> destinationIds, UUID id) {
        String upper = code.trim().toUpperCase(Locale.ROOT);
        if (id == null ? badgeRepository.existsByCodeIgnoreCase(upper) : badgeRepository.existsByCodeIgnoreCaseAndIdNot(upper, id)) {
            throw new InvalidBadgeException("Another badge already has the code " + upper);
        }
        if (type.hasLevels() && level == null) {
            throw new InvalidBadgeException("A " + type + " badge needs a level: SILVER, GOLDEN or PLATINUM");
        }
        if (!type.hasLevels() && (destinationIds == null || destinationIds.isEmpty())) {
            throw new InvalidBadgeException("A collection badge names the destinations to stamp");
        }
    }

    private static Set<UUID> destinationsOf(BadgeType type, Collection<UUID> destinationIds) {
        return type == BadgeType.DESTINATION_STAMPS && destinationIds != null ? new HashSet<>(destinationIds) : new HashSet<>();
    }

    private Badge find(UUID id) {
        return badgeRepository.findById(id).orElseThrow(BadgeNotFoundException::new);
    }
}
