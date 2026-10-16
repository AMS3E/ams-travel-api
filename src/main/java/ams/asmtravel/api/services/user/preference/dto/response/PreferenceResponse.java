package ams.asmtravel.api.services.user.preference.dto.response;

import ams.asmtravel.api.services.user.preference.domain.DistanceUnit;
import ams.asmtravel.api.services.user.preference.domain.Theme;

import java.time.Instant;
import java.util.UUID;

/** A person's preferences; the defaults, with no {@code updatedAt}, until they save any. */
public record PreferenceResponse(
        String preferredLanguage,
        String preferredCurrency,
        UUID preferredRegionId,
        boolean notificationEmail,
        boolean notificationPush,
        Theme theme,
        DistanceUnit distanceUnit,
        Instant updatedAt
) {
}
