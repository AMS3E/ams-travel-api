package ams.asmtravel.api.services.favorite.dto.response;

import ams.asmtravel.api.services.favorite.domain.FavoriteTarget;

import java.time.Instant;
import java.util.UUID;

public record FavoriteResponse(
        FavoriteTarget targetType,
        UUID targetId,
        Instant createdAt
) {
}
