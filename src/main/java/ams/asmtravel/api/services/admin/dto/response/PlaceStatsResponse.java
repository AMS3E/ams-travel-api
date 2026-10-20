package ams.asmtravel.api.services.admin.dto.response;

import java.time.LocalDate;

/**
 * One place's standing, for the dashboard's Destination Detail: an attraction,
 * a stay or a food place, told apart by {@code place.type}. The same figures
 * and period as {@link DestinationStatsResponse}, the Verified standing
 * included for every kind.
 */
public record PlaceStatsResponse(LocalDate from, LocalDate to, DestinationStatsResponse.Place place) {
}
