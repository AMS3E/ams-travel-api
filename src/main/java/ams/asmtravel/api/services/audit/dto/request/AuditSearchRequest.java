package ams.asmtravel.api.services.audit.dto.request;

import ams.asmtravel.api.services.audit.domain.AuditAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

/**
 * Every filter optional.
 *
 * @param entityType the entity's class name, {@code Destination}
 * @param userId     who made the change
 * @param limit      newest first; null for 50
 */
public record AuditSearchRequest(
        String entityType,
        UUID entityId,
        AuditAction action,
        UUID userId,
        @Min(1) @Max(200) Integer limit
) {
    public int size() {
        return limit == null ? 50 : limit;
    }
}
