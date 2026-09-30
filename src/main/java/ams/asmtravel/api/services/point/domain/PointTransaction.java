package ams.asmtravel.api.services.point.domain;

import ams.asmtravel.api.services.point.domain.PointSourceType;
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
import java.time.LocalDate;
import java.util.UUID;

/** One point earned; the unique key allows one per person, place, day and season, whatever earned it. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "gamification", name = "point_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_point_transactions_daily_target",
                columnNames = {"user_id", "target_id", "target_type", "reward_date", "season_id"}))
public class PointTransaction {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String targetType;

    @Column(nullable = false)
    private UUID targetId;

    /** What earned it: a rating or a review. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointSourceType sourceType;

    /** The rating or review that earned it. */
    @Column(nullable = false)
    private UUID sourceId;

    @Column(nullable = false)
    private UUID seasonId;

    @Column(nullable = false)
    private Integer points;

    /** The day in Cambodia it was earned. */
    @Column(nullable = false)
    private LocalDate rewardDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
