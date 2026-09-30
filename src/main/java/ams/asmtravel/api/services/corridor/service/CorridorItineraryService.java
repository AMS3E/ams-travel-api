package ams.asmtravel.api.services.corridor.service;

import ams.asmtravel.api.services.corridor.dto.request.CorridorItineraryRequest;
import ams.asmtravel.api.services.corridor.dto.request.CorridorStopRequest;
import ams.asmtravel.api.services.corridor.dto.response.CorridorItineraryResponse;
import ams.asmtravel.api.services.corridor.dto.response.CorridorStopResponse;

import java.util.List;
import java.util.UUID;

/**
 * Reads and rewrites a corridor's days and stops.
 * A rewrite keeps unchanged legs; new legs are routed after commit.
 */
public interface CorridorItineraryService {

    /** @throws org.springframework.web.server.ResponseStatusException 404 if missing or a draft the caller may not see */
    CorridorItineraryResponse getItinerary(UUID corridorId);

    /**
     * Replaces the days and stops.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 for a bad destination, bad times, or an unpublished stop on a published corridor
     */
    void replaceItinerary(UUID corridorId, CorridorItineraryRequest request);

    /** Flat stops of one corridor, or of all visible ones. */
    List<CorridorStopResponse> getStops(UUID corridorId);

    /**
     * Replaces the stops with one day visiting these destinations in order.
     *
     * @throws org.springframework.web.server.ResponseStatusException 409 for a multi-day corridor
     */
    void replaceStops(UUID corridorId, CorridorStopRequest request);
}
