package ams.asmtravel.api.services.activity.popular.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** A page a visitor opened. Rows outlive the place and the person, so counts stay. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "engagement", name = "browse_events")
public class BrowseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The signed-in visitor, if there was one. */
    private UUID userId;

    /** The place opened, when the event is about one. */
    private UUID destinationId;

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
