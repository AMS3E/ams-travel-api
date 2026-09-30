package ams.asmtravel.api.services.season.service.impl;

import ams.asmtravel.api.services.season.domain.Season;
import ams.asmtravel.api.services.season.domain.SeasonStatus;
import ams.asmtravel.api.services.season.domain.UserSeasonProgress;
import ams.asmtravel.api.services.season.dto.request.CreateSeasonRequest;
import ams.asmtravel.api.services.season.dto.request.UpdateSeasonRequest;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import ams.asmtravel.api.services.season.dto.response.UserSeasonProgressResponse;
import ams.asmtravel.api.services.season.exception.InvalidSeasonException;
import ams.asmtravel.api.services.season.exception.SeasonNotFoundException;
import ams.asmtravel.api.services.season.mapper.SeasonMapper;
import ams.asmtravel.api.services.season.repository.SeasonRepository;
import ams.asmtravel.api.services.season.repository.UserSeasonProgressRepository;
import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Season calendar and lifecycle, and each person's totals per season. Enabled seasons must not overlap. */
@Service
public class SeasonServiceImpl implements SeasonService {

    private final SeasonRepository seasonRepository;
    private final UserSeasonProgressRepository progressRepository;
    private final SeasonMapper mapper;
    /** Season boundaries are calendar dates in Cambodia, whatever the server's zone. */
    private final ZoneId zone;

    public SeasonServiceImpl(SeasonRepository seasonRepository, UserSeasonProgressRepository progressRepository,
                             SeasonMapper mapper, @Value("${app.seasons.zone:Asia/Phnom_Penh}") String zone) {
        this.seasonRepository = seasonRepository;
        this.progressRepository = progressRepository;
        this.mapper = mapper;
        this.zone = ZoneId.of(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeasonResponse> getAll() {
        return seasonRepository.findAllByOrderByStartsOnDesc().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeasonResponse findById(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public SeasonResponse create(CreateSeasonRequest request) {
        boolean enabled = request.isActive() == null || request.isActive();
        check(request.name(), request.startsOn(), request.endsOn(), enabled, null);
        Season season = mapper.toSeason(request);
        season.setName(request.name().trim());
        season.setIsActive(enabled);
        season = seasonRepository.saveAndFlush(season);
        syncStatuses();
        return mapper.toResponse(season);
    }

    @Override
    @Transactional
    public SeasonResponse update(UUID id, UpdateSeasonRequest request) {
        Season season = find(id);
        boolean enabled = request.isActive() == null ? season.getIsActive() : request.isActive();
        check(request.name(), request.startsOn(), request.endsOn(), enabled, id);
        mapper.apply(request, season);
        season.setName(request.name().trim());
        season.setIsActive(enabled);
        syncStatuses();
        return mapper.toResponse(season);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Season season = find(id);
        if (season.getStatus() != SeasonStatus.UPCOMING) {
            throw new InvalidSeasonException("Only an upcoming season can be deleted; a season that ran keeps its history");
        }
        seasonRepository.delete(season);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SeasonResponse> findCurrent() {
        return activeSeason().map(mapper::toResponse);
    }

    @Override
    @Transactional
    public SeasonResponse activate(UUID id) {
        Season season = find(id);
        if (!Boolean.TRUE.equals(season.getIsActive())) {
            throw new InvalidSeasonException("A disabled season cannot become the active one");
        }
        if (season.getStatus() == SeasonStatus.COMPLETED) {
            throw new InvalidSeasonException("A completed season cannot start again");
        }
        activeSeason().filter(active -> !active.getId().equals(id))
                .ifPresent(previous -> previous.setStatus(SeasonStatus.COMPLETED));
        season.setStatus(SeasonStatus.ACTIVE);
        return mapper.toResponse(season);
    }

    @Override
    @Transactional
    public SeasonResponse complete(UUID id) {
        Season season = find(id);
        if (season.getStatus() != SeasonStatus.ACTIVE) {
            throw new InvalidSeasonException("Only the active season can be completed");
        }
        season.setStatus(SeasonStatus.COMPLETED);
        return mapper.toResponse(season);
    }

    /** At startup and every midnight in Cambodia. */
    @Override
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${app.seasons.sync-cron:0 1 0 * * *}", zone = "${app.seasons.zone:Asia/Phnom_Penh}")
    public void syncStatuses() {
        LocalDate today = today();
        for (Season season : seasonRepository.findAll()) {
            if (season.getStatus() != SeasonStatus.COMPLETED && season.getEndsOn().isBefore(today)) {
                season.setStatus(SeasonStatus.COMPLETED);
            }
        }
        if (activeSeason().isEmpty()) {
            seasonRepository.findCovering(today).stream()
                    .filter(season -> season.getStatus() == SeasonStatus.UPCOMING)
                    .findFirst()
                    .ifPresent(season -> season.setStatus(SeasonStatus.ACTIVE));
        }
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(zone);
    }

    @Override
    public Window windowOf(LocalDate firstDay, LocalDate lastDay) {
        return new Window(firstDay.atStartOfDay(zone).toInstant(), lastDay.plusDays(1).atStartOfDay(zone).toInstant());
    }

    // Progress

    @Override
    @Transactional
    public void addProgress(UUID userId, UUID seasonId, int points, int ratings, int reviews,
                            int newRatedTargets, int newReviewedTargets) {
        Instant now = Instant.now();
        progressRepository.insertIfAbsent(UUID.randomUUID(), userId, seasonId, now);
        progressRepository.add(userId, seasonId, points, ratings, reviews, newRatedTargets, newReviewedTargets, now);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSeasonProgressResponse findProgress(UUID userId, UUID seasonId) {
        Season season = seasonId == null
                ? activeSeason().orElseThrow(() -> new SeasonNotFoundException("No season is active"))
                : find(seasonId);
        return mapper.toProgressResponse(season,
                progressRepository.findByUserIdAndSeasonId(userId, season.getId()).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSeasonProgressResponse> findHistory(UUID userId) {
        List<UserSeasonProgress> rows = progressRepository.findByUserIdOrderByStartedAtDesc(userId);
        Map<UUID, Season> seasons = seasonRepository.findAllById(rows.stream().map(UserSeasonProgress::getSeasonId).toList())
                .stream().collect(Collectors.toMap(Season::getId, Function.identity()));
        return rows.stream()
                .filter(row -> seasons.containsKey(row.getSeasonId()))
                .map(row -> mapper.toProgressResponse(seasons.get(row.getSeasonId()), row))
                .toList();
    }

    /** A person's totals are personal data: they go with the person. */
    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        progressRepository.deleteByUser(event.userId());
    }

    private Optional<Season> activeSeason() {
        return seasonRepository.findByStatus(SeasonStatus.ACTIVE).stream().findFirst();
    }

    private void check(String name, LocalDate startsOn, LocalDate endsOn, boolean enabled, UUID id) {
        if (endsOn.isBefore(startsOn)) {
            throw new InvalidSeasonException(HttpStatus.BAD_REQUEST, "A season cannot end before it starts");
        }
        String trimmed = name.trim();
        if (id == null ? seasonRepository.existsByNameIgnoreCase(trimmed) : seasonRepository.existsByNameIgnoreCaseAndIdNot(trimmed, id)) {
            throw new InvalidSeasonException("Another season is already called " + trimmed);
        }
        if (id == null ? seasonRepository.existsByStartsOn(startsOn) : seasonRepository.existsByStartsOnAndIdNot(startsOn, id)) {
            throw new InvalidSeasonException("Another season already starts on " + startsOn);
        }
        // Disabled seasons may overlap.
        if (enabled && seasonRepository.findOverlapping(startsOn, endsOn).stream().anyMatch(other -> !other.getId().equals(id))) {
            throw new InvalidSeasonException("Those dates overlap another season");
        }
    }

    private Season find(UUID id) {
        return seasonRepository.findById(id).orElseThrow(SeasonNotFoundException::new);
    }
}
