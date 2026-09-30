package ams.asmtravel.api.services.audit.service;

import ams.asmtravel.api.services.audit.dto.request.AuditSearchRequest;
import ams.asmtravel.api.services.audit.dto.response.AuditLogResponse;

import java.time.Instant;
import java.util.List;

/** Audit search, age-based retention, and full control's clear of the whole log. */
public interface AuditService {

    /** Newest first. */
    List<AuditLogResponse> search(AuditSearchRequest request);

    /** Drops entries older than the retention; nothing when it is zero. Runs nightly. */
    void pruneExpired();

    /** Prunes before the given cutoff, so tests need not wait. */
    int prune(Instant cutoff);

    /** Empties the log and writes one entry saying who did and how many went; answers that entry. */
    AuditLogResponse clearAll();

    /** How long an entry is kept; zero keeps it for good. */
    int retentionDays();
}
