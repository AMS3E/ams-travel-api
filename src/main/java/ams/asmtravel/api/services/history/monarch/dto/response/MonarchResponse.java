package ams.asmtravel.api.services.history.monarch.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MonarchResponse(
        UUID id,
        String name,
        String title,
        Integer reignStartYear,
        Integer reignEndYear,
        String description,
        String imageKey,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
