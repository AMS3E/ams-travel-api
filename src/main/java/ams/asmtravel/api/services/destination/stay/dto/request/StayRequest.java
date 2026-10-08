package ams.asmtravel.api.services.destination.stay.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.stay.domain.BreakfastOption;
import ams.asmtravel.api.services.destination.stay.domain.ParkingOption;
import ams.asmtravel.api.services.destination.stay.domain.PetsPolicy;
import ams.asmtravel.api.services.destination.stay.domain.SmokingPolicy;
import ams.asmtravel.api.services.destination.stay.domain.StayType;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A stay, for create and edit.
 *
 * @param destinationId optional; travellers find a stay by its map pin
 * @param ownerUserId   a partner may name only themselves, an editor anyone
 * @param latitude      required: the map pin is how travellers find the stay
 * @param slogan        one line someone said about it, quoted on its page; sloganBy says who
 * @param provinceId    the province the pin is in, filled from the pin on the dashboard; optional
 * @param isFeatured    used only from an editor; null keeps it on an edit
 * @param policies      the stay's policies in the host's own words, as Markdown; optional
 * @param status        null keeps it (new is DRAFT); only an editor may set PUBLISHED
 * @param interestIds   Stays-group interests; null keeps them on an edit
 */
public record StayRequest(
        UUID destinationId,
        UUID ownerUserId,
        @NotBlank @Size(max = 255) String name,
        @NotNull StayType type,
        @DecimalMin("0") @DecimalMax("5") @Digits(integer = 1, fraction = 1) BigDecimal starRating,
        @Size(max = 500) String shortDescription,
        String description,
        @Size(max = 300) String slogan,
        @Size(max = 120) String sloganBy,
        UUID provinceId,
        Boolean isFeatured,
        String policies,
        LocalTime checkInFrom,
        LocalTime checkOutUntil,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 512) String website,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal priceFrom,
        @Pattern(regexp = "^$|^[A-Z]{3}$", message = "Use a currency code such as USD") String currency,
        List<@NotBlank @Size(max = 100) String> amenities,
        @MediaKey(allowEmpty = true, message = "Upload the cover first, then send its key")
        String coverKey,
        String address,
        @NotNull(message = PIN_REQUIRED) @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull(message = PIN_REQUIRED) @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        ListingStatus status,
        Set<UUID> interestIds,
        LocalTime checkInUntil,
        LocalTime checkOutFrom,
        @Min(0) @Max(99) Integer minCheckInAge,
        Boolean childrenAllowed,
        PetsPolicy petsPolicy,
        SmokingPolicy smokingPolicy,
        Boolean partiesAllowed,
        @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$", message = "Quiet hours look like 22:00-07:00")
        String quietHours,
        BreakfastOption breakfastOption,
        List<@NotBlank @Size(max = 50) String> breakfastTypes,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal breakfastPrice,
        ParkingOption parkingOption,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal parkingPrice,
        List<@NotBlank @Size(max = 50) String> paymentMethods,
        List<@NotBlank @Size(max = 10) String> languagesSpoken,
        @Size(max = 100) String telegram,
        @Size(max = 50) String whatsapp,
        @Size(max = 512) String facebookUrl,
        @Size(max = 100) String licenseNumber,
        @Min(1800) @Max(2100) Integer yearOpened,
        List<@NotBlank @Size(max = 100) String> specialties
) {

    static final String PIN_REQUIRED = "Put the stay on the map: travellers find it by its pin";
}
