package ams.asmtravel.api.services.corridor.service;

import ams.asmtravel.api.services.corridor.dto.request.CorridorRoutePreviewRequest;
import ams.asmtravel.api.services.corridor.dto.response.CorridorRoutePreviewResponse;

import java.util.UUID;

/**
 * Calculates and stores a corridor's road route (Google if keyed, else OSM), only when the stored one is stale.
 * Read, call and write run in separate transactions; a result for changed stops is dropped.
 */
public interface CorridorRouteService {

    /** True when Google or OSM routing is configured. */
    boolean isConfigured();

    /** Calculates the route if stale; a failure is stored on the corridor, not thrown. */
    void calculateRoute(UUID corridorId);

    /**
     * Calculates the route now, even if current.
     *
     * @throws ams.asmtravel.api.services.corridor.exception.CorridorRouteException when it cannot be calculated
     */
    void recalculateRoute(UUID corridorId);

    /** Calculates every missing or stale route. */
    void calculateMissingRoutes();

    /**
     * Roads between unsaved stops for the planning map; stores nothing. A bad leg stays empty, the rest still work.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 when a destination does not exist
     */
    CorridorRoutePreviewResponse previewRoute(CorridorRoutePreviewRequest request);
}
