package ams.asmtravel.api.services.activity.popular.domain;

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
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** One opening of a place's page by a visitor, signed in or not. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "engagement", name = "browse_events")
public class BrowseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID userId;

    /** The kind of page opened; every row before the Popular page counted stays and food was a destination's. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BrowseTarget targetType = BrowseTarget.DESTINATION;

    private UUID targetId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(length = 100)
    private String sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
