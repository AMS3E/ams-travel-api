package ams.asmtravel.api.services.activity.stamp.dto.request;

import ams.asmtravel.api.services.activity.stamp.domain.StampTarget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Issue a destination's check-in code; the codes already posted keep working.
 *
 * @param label where the print will be posted ("Main entrance", "Brochure 2026"), to tell prints apart
 */
public record StampCodeRequest(@NotNull StampTarget targetType, @NotNull UUID targetId, @Size(max = 80) String label) {
}
