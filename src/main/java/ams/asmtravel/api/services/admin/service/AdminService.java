package ams.asmtravel.api.services.admin.service;

import ams.asmtravel.api.services.admin.dto.request.OverviewRequest;
import ams.asmtravel.api.services.admin.dto.response.DestinationStatsResponse;
import ams.asmtravel.api.services.admin.dto.response.OverviewResponse;
import ams.asmtravel.api.services.admin.dto.response.PlaceStatsResponse;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.UUID;

/** The dashboards' summary tiles; the admin service owns no rows. */
public interface AdminService {

    /** Reviews, the current badge period and the latest reviews; table counts too, for full control. */
    OverviewResponse overview(OverviewRequest request, Authentication authentication);

    /**
     * Every destination's ratings, reviews and browses over a period, with its
     * views, Verified standing and status as of today.
     *
     * @param from the period's first day in Cambodia, inclusive; null with {@code to} null for all time
     * @param to   its last day, inclusive; null for up to today
     */
    DestinationStatsResponse destinationStats(LocalDate from, LocalDate to);

    /**
     * One place's standing by its id, an attraction, a stay or a food place,
     * over the same period; 404 when no place has the id.
     */
    PlaceStatsResponse placeStats(UUID id, LocalDate from, LocalDate to);
}
