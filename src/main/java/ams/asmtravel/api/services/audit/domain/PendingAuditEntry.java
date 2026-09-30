package ams.asmtravel.api.services.audit.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** One captured write, on its way to the log. */
public record PendingAuditEntry(
        UUID userId,
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
