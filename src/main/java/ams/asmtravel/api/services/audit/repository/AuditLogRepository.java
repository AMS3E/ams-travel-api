package ams.asmtravel.api.services.audit.repository;

import ams.asmtravel.api.services.audit.domain.AuditAction;
import ams.asmtravel.api.services.audit.domain.AuditLog;
import ams.asmtravel.api.services.audit.domain.PendingAuditEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

/**
 * Audit log data access. Writes use plain JDBC on the change's connection,
 * because the listener runs inside a Hibernate flush; reads and trims use JPA.
 */
@Repository
public class AuditLogRepository {

    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String insert;

    public AuditLogRepository(EntityManager entityManager, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                              DataSource dataSource) {
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        // PostgreSQL takes jsonb and inet by cast; H2, in the tests, reads JSON text with FORMAT JSON.
        boolean postgres = productName(dataSource).toLowerCase().contains("postgres");
        String json = postgres ? "cast(? as jsonb)" : "? format json";
        this.insert = "insert into system.audit_logs"
                + " (user_id, action, entity_type, entity_id, old_data, new_data, ip_address, user_agent, created_at)"
                + " values (?, ?, ?, ?, " + json + ", " + json + ", " + (postgres ? "cast(? as inet)" : "?") + ", ?, ?)";
    }

    public void write(PendingAuditEntry entry) {
        jdbcTemplate.update(insert,
                entry.userId(),
                entry.action().name(),
                entry.entityType(),
                entry.entityId(),
                json(entry.oldData()),
                json(entry.newData()),
                entry.ipAddress(),
                entry.userAgent(),
                Timestamp.from(entry.createdAt()));
    }

    /** Newest first, every filter optional; the id breaks ties between entries of one transaction. */
    public List<AuditLog> search(String entityType, UUID entityId, AuditAction action, UUID userId, int limit) {
        CriteriaBuilder where = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLog> query = where.createQuery(AuditLog.class);
        Root<AuditLog> entry = query.from(AuditLog.class);
        List<Predicate> filters = new ArrayList<>();
        if (entityType != null) {
            filters.add(where.equal(entry.get("entityType"), entityType));
        }
        if (entityId != null) {
            filters.add(where.equal(entry.get("entityId"), entityId));
        }
        if (action != null) {
            filters.add(where.equal(entry.get("action"), action));
        }
        if (userId != null) {
            filters.add(where.equal(entry.get("userId"), userId));
        }
        query.where(filters.toArray(Predicate[]::new))
                .orderBy(where.desc(entry.get("createdAt")), where.desc(entry.get("id")));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }

    /** Deletes everything before {@code cutoff} in one bulk statement; this is the largest table. */
    public int deleteCreatedBefore(Instant cutoff) {
        return entityManager.createQuery("delete from AuditLog a where a.createdAt < :cutoff")
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }

    public int deleteEverything() {
        return entityManager.createQuery("delete from AuditLog a").executeUpdate();
    }

    private String json(Map<String, Object> data) {
        return data == null || data.isEmpty() ? null : objectMapper.writeValueAsString(data);
    }

    private static String productName(DataSource dataSource) {
        try {
            String name = JdbcUtils.extractDatabaseMetaData(dataSource, metaData -> metaData.getDatabaseProductName());
            return name == null ? "" : name;
        } catch (Exception unknown) {
            return "";
        }
    }
}
