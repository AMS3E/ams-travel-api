package ams.asmtravel.api.services.destination.stayroom.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The dates and guests a price preview is for, as query parameters. Dates default to tonight in Cambodia;
 * rooms to one, adults to two, children to none.
 *
 * @param stayId the stay, required for the stay-wide preview; ignored for one room's
 */
public record StayQuoteRequest(
        UUID stayId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        @Min(1) @Max(10) Integer rooms,
        @Min(1) @Max(40) Integer adults,
        @Min(0) @Max(20) Integer children
) {
}
