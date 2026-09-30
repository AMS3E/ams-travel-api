package ams.asmtravel.api.services.season.dto.request;

import ams.asmtravel.api.services.season.domain.SeasonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** @param isActive null keeps it as it is */
public record UpdateSeasonRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull SeasonType type,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        Boolean isActive
) {
}
