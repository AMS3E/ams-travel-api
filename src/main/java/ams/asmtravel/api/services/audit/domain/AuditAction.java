package ams.asmtravel.api.services.audit.domain;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,

    /** The log was cleared; written in the same transaction, so a cleared log is never blank. */
    PURGE
}
