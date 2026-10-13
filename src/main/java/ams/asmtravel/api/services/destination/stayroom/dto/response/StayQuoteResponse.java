package ams.asmtravel.api.services.destination.stayroom.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Every room on sale at a stay, priced for the nights and guests asked.
 *
 * @param weekendNights the nights the stay counts as weekend, named by the day they start
 * @param fromTotal     the cheapest bookable room's total: "from US$576 for 2 nights"; null when none fits
 */
public record StayQuoteResponse(
        UUID stayId,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        int rooms,
        int adults,
        int children,
        String currency,
        List<String> weekendNights,
        BigDecimal fromTotal,
        List<RoomQuoteResponse> roomQuotes
) {
}
