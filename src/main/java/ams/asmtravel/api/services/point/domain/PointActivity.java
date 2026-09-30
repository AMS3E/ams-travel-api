package ams.asmtravel.api.services.point.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A place a person rated or reviewed in a season, once: what the season's unique counts are made of. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "gamification", name = "point_activities",
        uniqueConstraints = @UniqueConstraint(name = "uk_point_activities",
                columnNames = {"user_id", "season_id", "source_type", "target_type", "target_id"}))
public class PointActivity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID seasonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointSourceType sourceType;

    @Column(nullable = false, length = 30)
    private String targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false, updatable = false)
    private Instant firstAt;
}
