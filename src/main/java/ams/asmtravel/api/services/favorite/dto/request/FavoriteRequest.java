package ams.asmtravel.api.services.favorite.dto.request;

import ams.asmtravel.api.services.favorite.domain.FavoriteTarget;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Saves a published place, or a corridor. */
public record FavoriteRequest(
        @NotNull FavoriteTarget targetType,
        @NotNull UUID targetId
) {
}
