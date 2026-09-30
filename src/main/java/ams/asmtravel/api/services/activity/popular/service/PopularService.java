package ams.asmtravel.api.services.activity.popular.service;

import ams.asmtravel.api.services.activity.popular.domain.BrowseTotal;
import ams.asmtravel.api.services.activity.popular.dto.request.BrowseEventRequest;
import ams.asmtravel.api.services.activity.popular.dto.response.PopularResponse;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Browses and the Popular list. A place counts once per visitor per day (Cambodia time). */
public interface PopularService {

    /** Keeps what the website saw; a place's first open of the day by a visitor counts as a view. */
    void record(BrowseEventRequest request, UUID userId);

    /** A signed-in visitor opened a destination; true if it is their first open of it today. */
    boolean recordBrowse(UUID userId, UUID destinationId);

    /**
     * Most-browsed published places lately, featured ones first.
     *
     * @param regionId only this zone or tourism region; null for everywhere
     * @param limit    how many; null for the configured size
     */
    PopularResponse popular(UUID regionId, Integer limit);

    // For other services.

    /** Different signed-in visitors per place over Popular's window, most first. */
    List<BrowseTotal> recentTotals();

    /** Places a visitor has opened, ever. */
    Set<UUID> browsedDestinationIds(UUID userId);

    /** Places a visitor opened in a window, each counted once a day: what the Browser badge counts. */
    long countDailyBrowses(UUID userId, Instant from, Instant to);
}
