package ams.asmtravel.api.services.activity.popular.service;

import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTotal;
import ams.asmtravel.api.services.activity.popular.dto.request.BrowseEventRequest;
import ams.asmtravel.api.services.activity.popular.dto.response.PopularResponse;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PopularService {

    void record(BrowseEventRequest request, UUID userId);

    /** A signed-in visitor opened a destination; true when it is their first time today in Cambodia. */
    boolean recordBrowse(UUID userId, UUID destinationId);

    /**
     * The Popular list of one kind (destinations when null), kept to any of the regions, provinces and
     * interests given, filled up to {@code limit} when browses alone do not reach it.
     */
    PopularResponse popular(BrowseTarget type, Set<UUID> regionIds, Set<UUID> provinceIds, Set<UUID> interestIds, Integer limit);

    // For other services: destinations, as before.

    List<BrowseTotal> recentTotals();

    List<BrowseTotal> recentTotals(BrowseTarget type);

    int windowDays();

    List<BrowseTotal> totalsBetween(Instant from, Instant to);

    ZoneId zone();

    Set<UUID> browsedDestinationIds(UUID userId);

    long countDailyBrowses(UUID userId, Instant from, Instant to);
}
