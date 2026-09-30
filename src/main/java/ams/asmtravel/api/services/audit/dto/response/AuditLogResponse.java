package ams.asmtravel.api.services.audit.dto.response;

import ams.asmtravel.api.services.audit.domain.AuditAction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * @param userName who made the change, as their profile reads now; null when unknown
 * @param oldData  the changed fields before (every field for a delete)
 * @param newData  the changed fields after (every field for a create)
 */
public record AuditLogResponse(
        Long id,
        UUID userId,
        String userName,
        String userEmail,
        AuditAction action,
        String entityType,
        UUID entityId,
        Map<String, Object> oldData,
        Map<String, Object> newData,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
}
