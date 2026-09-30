package ams.asmtravel.api.services.destination.stayroom.dto.response;

import ams.asmtravel.api.services.destination.stayroom.domain.RoomBathroom;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomHousekeeping;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomPaymentTerms;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomType;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param sleeps        people the beds sleep
 * @param currency      the stay's currency, used by {@code pricePerNight}
 * @param photoUrls     links for {@code photoKeys}, in the same order
 */
public record StayRoomResponse(
        UUID id,
        UUID stayId,
        String name,
        RoomType roomType,
        Integer roomCount,
        Integer singleBeds,
        Integer doubleBeds,
        Integer queenBeds,
        Integer kingBeds,
        Integer bunkBeds,
        Integer sofaBeds,
        Integer futonBeds,
        Integer sleeps,
        Integer maxAdults,
        Integer maxChildren,
        BigDecimal sizeSqm,
        RoomView view,
        RoomBathroom bathroom,
        List<String> amenities,
        Boolean smokingAllowed,
        BigDecimal pricePerNight,
        String currency,
        Boolean breakfastIncluded,
        Integer freeCancellationDays,
        String description,
        List<String> photoKeys,
        List<String> photoUrls,
        Integer sortOrder,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        String highlight,
        Boolean hasWindow,
        Integer extraBeds,
        BigDecimal extraBedPrice,
        BigDecimal highSeasonPrice,
        Boolean taxesIncluded,
        Integer minNights,
        RoomPaymentTerms paymentTerms,
        Integer bedrooms,
        Integer livingRooms,
        Integer bathroomCount,
        Integer floor,
        Boolean liftAccess,
        RoomHousekeeping housekeeping,
        Integer cots,
        BigDecimal cotPrice,
        Integer childrenFreeUnderAge,
        BigDecimal weeklyPrice,
        BigDecimal monthlyPrice,
        Integer depositPercent
) {
}
