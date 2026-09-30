package ams.asmtravel.api.services.season.dto.request;

import ams.asmtravel.api.services.season.domain.SeasonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** A new season; it starts UPCOMING, and a null isActive means true. */
public record CreateSeasonRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull SeasonType type,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        Boolean isActive
) {
}
