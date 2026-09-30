package ams.asmtravel.api.services.history.religion.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReligionResponse(
        UUID id,
        String name,
        String description,
        String iconKey,
        Instant createdAt
) {
}
