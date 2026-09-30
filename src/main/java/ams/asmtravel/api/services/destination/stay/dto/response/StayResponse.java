package ams.asmtravel.api.services.destination.stay.dto.response;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.stay.domain.BreakfastOption;
import ams.asmtravel.api.services.destination.stay.domain.ParkingOption;
import ams.asmtravel.api.services.destination.stay.domain.PetsPolicy;
import ams.asmtravel.api.services.destination.stay.domain.SmokingPolicy;
import ams.asmtravel.api.services.destination.stay.domain.StayType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** @param priceFrom the lowest nightly price, in {@code currency} */
public record StayResponse(
        UUID id,
        UUID destinationId,
        UUID ownerUserId,
        String name,
        StayType type,
        BigDecimal starRating,
        String shortDescription,
        String description,
        LocalTime checkInFrom,
        LocalTime checkOutUntil,
        String phone,
        String contactEmail,
        String website,
        BigDecimal priceFrom,
        String currency,
        List<String> amenities,
        String coverKey,
        String coverUrl,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        ListingStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<UUID> interestIds,
        LocalTime checkInUntil,
        LocalTime checkOutFrom,
        Integer minCheckInAge,
        Boolean childrenAllowed,
        PetsPolicy petsPolicy,
        SmokingPolicy smokingPolicy,
        Boolean partiesAllowed,
        String quietHours,
        BreakfastOption breakfastOption,
        List<String> breakfastTypes,
        BigDecimal breakfastPrice,
        ParkingOption parkingOption,
        BigDecimal parkingPrice,
        List<String> paymentMethods,
        List<String> languagesSpoken,
        String telegram,
        String whatsapp,
        String facebookUrl,
        String licenseNumber,
        Integer yearOpened,
        List<String> specialties
) {
}
