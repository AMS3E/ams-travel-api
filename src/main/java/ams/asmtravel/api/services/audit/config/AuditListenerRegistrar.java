package ams.asmtravel.api.services.audit.config;

import ams.asmtravel.api.services.audit.service.impl.AuditServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Registers the audit listener with Hibernate; only its events say which fields changed and from what. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditListenerRegistrar {

    private final EntityManagerFactory entityManagerFactory;
    private final AuditServiceImpl auditEventListener;

    @PostConstruct
    void register() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry()
                .requireService(EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_INSERT, auditEventListener);
        registry.appendListeners(EventType.POST_UPDATE, auditEventListener);
        registry.appendListeners(EventType.POST_DELETE, auditEventListener);
    }
}
