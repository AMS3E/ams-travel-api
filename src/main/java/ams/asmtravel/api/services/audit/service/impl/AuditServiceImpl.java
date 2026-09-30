package ams.asmtravel.api.services.audit.service.impl;

import ams.asmtravel.api.services.audit.config.AuditProperties;
import ams.asmtravel.api.services.audit.domain.AuditAction;
import ams.asmtravel.api.services.audit.domain.AuditLog;
import ams.asmtravel.api.services.audit.domain.AuditScope;
import ams.asmtravel.api.services.audit.domain.PendingAuditEntry;
import ams.asmtravel.api.services.audit.dto.request.AuditSearchRequest;
import ams.asmtravel.api.services.audit.dto.response.AuditLogResponse;
import ams.asmtravel.api.services.audit.mapper.AuditLogMapper;
import ams.asmtravel.api.services.audit.repository.AuditLogRepository;
import ams.asmtravel.api.services.audit.service.AuditService;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.user.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.Type;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Captures, searches, trims and clears the audit log. Capture is one Hibernate listener,
 * so no service can forget it; bulk statements are not captured.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService, PostInsertEventListener, PostUpdateEventListener,
        PostDeleteEventListener {

    private static final int LONGEST_VALUE = 1000;
    private static final Pattern ADDRESS = Pattern.compile("^(?:\\d{1,3}(?:\\.\\d{1,3}){3}|[0-9A-Fa-f:]{2,39})$");

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper mapper;
    private final AuditProperties properties;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(AuditSearchRequest request) {
        String entityType = request.entityType() == null || request.entityType().isBlank() ? null : request.entityType().trim();
        return respond(auditLogRepository.search(entityType, request.entityId(), request.action(), request.userId(), request.size()));
    }

    /** In the small hours, when a bulk delete competes with nobody; two instances doing it is harmless. */
    @Override
    @Scheduled(cron = "${app.audit.retention.cron:0 30 3 * * *}")
    @Transactional
    public void pruneExpired() {
        if (retentionDays() == 0) {
            return;
        }
        int removed = prune(Instant.now().minus(Duration.ofDays(retentionDays())));
        if (removed > 0) {
            log.info("Audit log: removed {} entries older than {} days", removed, retentionDays());
        }
    }

    @Override
    @Transactional
    public int prune(Instant cutoff) {
        return auditLogRepository.deleteCreatedBefore(cutoff);
    }

    @Override
    @Transactional
    public AuditLogResponse clearAll() {
        int removed = auditLogRepository.deleteEverything();
        UUID userId = userService.resolvedUserId().orElse(null);
        auditLogRepository.write(new PendingAuditEntry(userId, AuditAction.PURGE, "AuditLog", null, null,
                Map.of("removed", removed), null, null, Instant.now()));
        log.warn("Audit log cleared: {} entries removed by user {}", removed, userId);
        return respond(auditLogRepository.search("AuditLog", null, AuditAction.PURGE, null, 1)).getFirst();
    }

    @Override
    public int retentionDays() {
        return Math.max(properties.retention().days(), 0);
    }

    // Capture (Hibernate listener callbacks).

    @Override
    public void onPostInsert(PostInsertEvent event) {
        capture(AuditAction.CREATE, event.getEntity(), event.getId(), null,
                values(event.getPersister(), event.getState(), null));
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        capture(AuditAction.DELETE, event.getEntity(), event.getId(),
                values(event.getPersister(), event.getDeletedState(), null), null);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        int[] dirty = event.getDirtyProperties();
        // Hibernate flushes an unchanged row as an update often enough that logging those would bury the edits.
        if (dirty == null || dirty.length == 0) {
            return;
        }
        Map<String, Object> after = values(event.getPersister(), event.getState(), dirty);
        if (after.isEmpty() && event.getOldState() == null) {
            return;
        }
        capture(AuditAction.UPDATE, event.getEntity(), event.getId(),
                event.getOldState() == null ? null : values(event.getPersister(), event.getOldState(), dirty), after);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private List<AuditLogResponse> respond(List<AuditLog> entries) {
        Map<UUID, UserSummary> people = userService.findSummaries(
                entries.stream().map(AuditLog::getUserId).filter(Objects::nonNull).toList());
        return entries.stream()
                .map(entry -> mapper.toResponse(entry, entry.getUserId() == null ? null : people.get(entry.getUserId())))
                .toList();
    }

    private void capture(AuditAction action, Object entity, Object id, Map<String, Object> before, Map<String, Object> after) {
        if (entity == null || !AuditScope.isAudited(entity.getClass(), id)) {
            return;
        }
        HttpServletRequest request = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
        auditLogRepository.write(new PendingAuditEntry(
                userService.resolvedUserId().orElse(null),
                action,
                entity.getClass().getSimpleName(),
                (UUID) id,
                before,
                after,
                request == null ? null : address(request),
                request == null ? null : request.getHeader("User-Agent"),
                Instant.now()));
    }

    /** The named properties' values (all when {@code only} is null), as JSON can hold them; collections are left out. */
    private static Map<String, Object> values(EntityPersister persister, Object[] state, int[] only) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (state == null) {
            return values;
        }
        String[] names = persister.getPropertyNames();
        Type[] types = persister.getPropertyTypes();
        int[] indexes = only != null ? only : IntStream.range(0, names.length).toArray();
        for (int index : indexes) {
            if (index < 0 || index >= names.length || index >= state.length || types[index].isCollectionType()) {
                continue;
            }
            values.put(names[index], plain(state[index], types[index]));
        }
        return values;
    }

    private static Object plain(Object value, Type type) {
        if (value == null || value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Double || value instanceof Float) {
            return value;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof CharSequence text) {
            String string = text.toString();
            return string.length() > LONGEST_VALUE ? string.substring(0, LONGEST_VALUE) + "…" : string;
        }
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        if (value instanceof UUID || value instanceof Temporal) {
            return value.toString();
        }
        if (value instanceof HibernateProxy proxy) {
            return String.valueOf(proxy.getHibernateLazyInitializer().getInternalIdentifier());
        }
        if (type.isEntityType()) {
            return entityId(value);
        }
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return value.toString();
    }

    /** A linked row, by its id when it has a getter for one. */
    private static Object entityId(Object entity) {
        try {
            Object id = entity.getClass().getMethod("getId").invoke(entity);
            return id == null ? null : id.toString();
        } catch (ReflectiveOperationException noId) {
            return null;
        }
    }

    private static String address(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String candidate = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return candidate != null && ADDRESS.matcher(candidate).matches() ? candidate : null;
    }
}
