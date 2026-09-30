package ams.asmtravel.api.services.history.destinationmonarch.dto.response;

import java.util.UUID;

/**
 * A monarch tied to a destination, with the monarch's details included.
 *
 * @param description how this monarch shaped the place
 */
public record DestinationMonarchResponse(
        UUID destinationId,
        UUID monarchId,
        String name,
        String title,
        Integer reignStartYear,
        Integer reignEndYear,
        String description
) {
}
