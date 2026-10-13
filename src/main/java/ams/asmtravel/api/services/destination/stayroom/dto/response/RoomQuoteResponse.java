package ams.asmtravel.api.services.destination.stayroom.dto.response;

import ams.asmtravel.api.services.destination.stayroom.domain.RoomQuote;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * One room priced for the nights asked: the app prints {@code total} "for N nights" and nothing per night.
 *
 * @param bookable priced, and the guests, the rooms and the nights all fit
 */
public record RoomQuoteResponse(
        UUID roomId,
        String name,
        String currency,
        BigDecimal pricePerNight,
        BigDecimal weekendPrice,
        int nights,
        int weekendNightCount,
        List<RoomQuote.Night> perNight,
        BigDecimal total,
        boolean fitsGuests,
        boolean enoughRooms,
        boolean meetsMinNights,
        int minNights,
        boolean breakfastIncluded,
        boolean taxesIncluded,
        boolean bookable
) {
}
