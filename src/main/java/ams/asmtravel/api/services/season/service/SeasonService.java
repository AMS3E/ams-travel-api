package ams.asmtravel.api.services.season.service;

import ams.asmtravel.api.services.season.dto.request.CreateSeasonRequest;
import ams.asmtravel.api.services.season.dto.request.UpdateSeasonRequest;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import ams.asmtravel.api.services.season.dto.response.UserSeasonProgressResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Cambodia's high and green seasons, their lifecycle, and each person's totals per season. */
public interface SeasonService {

    /** Days as an instant range for timestamp queries: from the first day's start to the day after the last. */
    record Window(Instant from, Instant until) {
    }

    /** Latest first. */
    List<SeasonResponse> getAll();

    SeasonResponse findById(UUID id);

    SeasonResponse create(CreateSeasonRequest request);

    SeasonResponse update(UUID id, UpdateSeasonRequest request);

    /** Only an UPCOMING season: one that ran keeps its history. */
    void delete(UUID id);

    /** The ACTIVE season, if any. */
    Optional<SeasonResponse> findCurrent();

    /** Makes this season ACTIVE; the one active before it is COMPLETED, its history kept. */
    SeasonResponse activate(UUID id);

    /** Ends the ACTIVE season early. */
    SeasonResponse complete(UUID id);

    /** Brings statuses in line with today: ended seasons COMPLETED, the one covering today ACTIVE. */
    void syncStatuses();

    /** Today in Cambodia, not in the server's zone. */
    LocalDate today();

    Window windowOf(LocalDate firstDay, LocalDate lastDay);

    // Progress

    /** Adds to a person's totals in a season, starting their row at zero when it is their first activity. */
    void addProgress(UUID userId, UUID seasonId, int points, int ratings, int reviews,
                     int newRatedTargets, int newReviewedTargets);

    /** A person's totals in a season, the ACTIVE one when {@code seasonId} is null. */
    UserSeasonProgressResponse findProgress(UUID userId, UUID seasonId);

    /** A person's totals in every season they took part in, newest first. */
    List<UserSeasonProgressResponse> findHistory(UUID userId);
}
