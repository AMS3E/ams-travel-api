package ams.asmtravel.api.services.destination.stayroom.dto.request;

import ams.asmtravel.api.services.destination.stayroom.domain.RoomBathroom;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomHousekeeping;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomPaymentTerms;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomType;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomView;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A room, for create and edit. Counts and flags are boxed so a missing one gets its default, not a 400.
 *
 * @param stayId               fixed once the room exists
 * @param amenities            slugs of the taxonomy's amenities offered in a room ({@code GET /api/v1/amenities?level=ROOM})
 * @param pricePerNight        the cheapest active room sets the stay's "from" price
 * @param freeCancellationDays null means non-refundable
 * @param photoKeys            photos from the stay's gallery; the first one leads
 * @param highSeasonPrice      a night from November to April
 * @param floor                ground floor is 0; null when it varies
 */
public record StayRoomRequest(
        @NotNull UUID stayId,
        @NotBlank @Size(max = 150) String name,
        @NotNull RoomType roomType,
        @Min(1) @Max(500) Integer roomCount,
        @Min(0) @Max(20) Integer singleBeds,
        @Min(0) @Max(20) Integer doubleBeds,
        @Min(0) @Max(20) Integer queenBeds,
        @Min(0) @Max(20) Integer kingBeds,
        @Min(0) @Max(20) Integer bunkBeds,
        @Min(0) @Max(20) Integer sofaBeds,
        @Min(0) @Max(20) Integer futonBeds,
        @Min(1) @Max(40) Integer maxAdults,
        @Min(0) @Max(20) Integer maxChildren,
        @DecimalMin("1") @DecimalMax("9999") @Digits(integer = 5, fraction = 1) BigDecimal sizeSqm,
        RoomView view,
        RoomBathroom bathroom,
        List<@NotBlank @Size(max = 100) String> amenities,
        Boolean smokingAllowed,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal pricePerNight,
        Boolean breakfastIncluded,
        @Min(0) @Max(90) Integer freeCancellationDays,
        String description,
        @Size(max = 12, message = "A room shows at most twelve photos")
        List<@MediaKey(message = "Choose the photos from the stay's gallery") String> photoKeys,
        Integer sortOrder,
        Boolean isActive,
        @Size(max = 150) String highlight,
        Boolean hasWindow,
        @Min(0) @Max(4) Integer extraBeds,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal extraBedPrice,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal highSeasonPrice,
        Boolean taxesIncluded,
        @Min(1) @Max(60) Integer minNights,
        RoomPaymentTerms paymentTerms,
        @Min(0) @Max(20) Integer bedrooms,
        @Min(0) @Max(10) Integer livingRooms,
        @Min(0) @Max(20) Integer bathroomCount,
        @Min(-2) @Max(200) Integer floor,
        Boolean liftAccess,
        RoomHousekeeping housekeeping,
        @Min(0) @Max(4) Integer cots,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal cotPrice,
        @Min(0) @Max(17) Integer childrenFreeUnderAge,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal weeklyPrice,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal monthlyPrice,
        @Min(1) @Max(100) Integer depositPercent
) {
}
