package ams.asmtravel.api.services.season.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** One person's totals in one season; a new season starts a new row from zero, old rows stay. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "gamification", name = "user_season_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_season_progress", columnNames = {"user_id", "season_id"}))
public class UserSeasonProgress {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID seasonId;

    @Column(nullable = false)
    private Integer points = 0;

    /** Ratings given this season (changing one does not count again). */
    @Column(nullable = false)
    private Integer ratingCount = 0;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Column(nullable = false)
    private Integer uniqueRatedTargetCount = 0;

    @Column(nullable = false)
    private Integer uniqueReviewedTargetCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
