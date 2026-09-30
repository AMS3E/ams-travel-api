package ams.asmtravel.api.services.history.history.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One moment in a destination's past.
 *
 * @param eventYear          CE; null when only a century or a range is known
 * @param historicalPeriodId the period it falls in; null for none
 */
public record DestinationHistoryRequest(
        @NotNull UUID destinationId,
        UUID historicalPeriodId,
        @NotBlank @Size(max = 255) String title,
        String description,
        Integer eventYear,
        LocalDate startDate,
        LocalDate endDate,
        String sourceReference
) {
}
