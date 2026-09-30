package ams.asmtravel.api.services.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One catalogue write: who, what, which row, and the changed values.
 * Read-only here; written by {@code AuditWriter} on the change's own connection.
 */
@Entity
@Immutable
@NoArgsConstructor
@Getter
@Table(schema = "system", name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who made the change; null for a write with nobody signed in, or a person since deleted. */
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction action;

    /** The entity's class name: {@code Destination}, {@code PublishRequest}. */
    @Column(nullable = false, length = 50)
    private String entityType;

    private UUID entityId;

    /** The changed fields' values before; every field's for a delete. */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> oldData;

    /** The changed fields' values after; every field's for a create. */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> newData;

    @Column(columnDefinition = "inet")
    private String ipAddress;

    @Column(columnDefinition = "text")
    private String userAgent;

    @Column(nullable = false)
    private Instant createdAt;
}
