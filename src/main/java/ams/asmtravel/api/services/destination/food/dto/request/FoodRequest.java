package ams.asmtravel.api.services.destination.food.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.destination.food.domain.AlcoholOption;
import ams.asmtravel.api.services.destination.food.domain.FoodParking;
import ams.asmtravel.api.services.destination.food.domain.FoodType;
import ams.asmtravel.api.services.destination.food.domain.ReservationPolicy;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A food listing, for create and edit.
 *
 * @param destinationId optional; travellers find a listing by its map pin
 * @param ownerUserId  a partner may name only themselves, an editor anyone
 * @param latitude     required: the map pin is how travellers find it
 * @param openingHours day ({@code mon}…{@code sun}) to hours ({@code 07:00-22:00})
 * @param status       null keeps it (new is DRAFT); only an editor may set PUBLISHED
 * @param isFeatured   used only from an editor
 */
public record FoodRequest(
        UUID destinationId,
        UUID ownerUserId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 150) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        @NotNull FoodType type,
        @Size(max = 500) String shortDescription,
        String description,
        String address,
        @NotNull(message = PIN_REQUIRED) @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull(message = PIN_REQUIRED) @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Pattern(regexp = "^$|^\\${1,4}$", message = "Use $ to $$$$") String priceRange,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal priceFrom,
        @Pattern(regexp = "^$|^[A-Z]{3}$", message = "Use a currency code such as USD") String currency,
        Map<@Pattern(regexp = "^(mon|tue|wed|thu|fri|sat|sun)$", message = "Days are mon to sun") String,
                @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-4]):[0-5]\\d$|^closed$",
                        message = "Hours look like 07:00-22:00, or closed") String> openingHours,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 255) String website,
        @MediaKey(allowEmpty = true, message = "Upload the cover first, then send its key")
        String coverKey,
        ListingStatus status,
        Boolean isFeatured,
        Boolean isLocalSpecialty,
        Set<UUID> categoryIds,
        Set<UUID> tagIds,
        Set<UUID> interestIds,
        List<@NotBlank @Size(max = 100) String> cuisines,
        List<@NotBlank @Size(max = 100) String> mealsServed,
        List<@NotBlank @Size(max = 100) String> dietary,
        List<@NotBlank @Size(max = 100) String> serviceOptions,
        List<@NotBlank @Size(max = 100) String> amenities,
        List<@NotBlank @Size(max = 100) String> goodFor,
        List<@NotBlank @Size(max = 10) String> menuLanguages,
        List<@NotBlank @Size(max = 100) String> deliveryPartners,
        List<@NotBlank @Size(max = 100) String> paymentMethods,
        List<@NotBlank @Size(max = 10) String> languagesSpoken,
        AlcoholOption alcohol,
        FoodParking parkingOption,
        ReservationPolicy reservations,
        @Min(1) @Max(5000) Integer seatingCapacity,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal averageMealPrice,
        @Size(max = 100) String telegram,
        @Size(max = 50) String whatsapp,
        @Size(max = 512) String facebookUrl,
        @Min(1800) @Max(2100) Integer yearOpened,
        List<@NotBlank @Size(max = 100) String> specialties
) {

    static final String PIN_REQUIRED = "Put the place on the map: travellers find it by its pin";
}
