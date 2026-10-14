package ams.asmtravel.api.services.user.preference.dto.request;

import ams.asmtravel.api.services.user.preference.domain.DistanceUnit;
import ams.asmtravel.api.services.user.preference.domain.Theme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Every preference at once, as the settings form sends them.
 *
 * @param preferredRegionId a region's id, or null for none
 */
public record PreferenceRequest(
        @NotBlank @Pattern(regexp = "^[a-z]{2}(?:-[A-Z]{2})?$", message = "Use a language code such as en or km")
        String preferredLanguage,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Use a currency code such as USD or KHR")
        String preferredCurrency,
        UUID preferredRegionId,
        @NotNull Boolean notificationEmail,
        @NotNull Boolean notificationPush,
        @NotNull Theme theme,
        /** KM or MI; left out, kilometres. */
        DistanceUnit distanceUnit
) {
}
