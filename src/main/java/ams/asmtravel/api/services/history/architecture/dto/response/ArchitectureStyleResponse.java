package ams.asmtravel.api.services.history.architecture.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ArchitectureStyleResponse(
        UUID id,
        String name,
        String description,
        UUID periodId,
        String imageKey,
        String imageUrl,
        Instant createdAt
) {
}
