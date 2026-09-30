package ams.asmtravel.api.services.badge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A person's progress on one badge in one season; earnedAt is set once. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "gamification", name = "user_badges",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_badges_user_badge_season",
                columnNames = {"user_id", "badge_id", "season_id"}))
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID seasonId;

    @Column(nullable = false)
    private Integer progress = 0;

    /** When it unlocked; null while still in progress. */
    private Instant earnedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    public UserBadge(UUID userId, Badge badge, UUID seasonId) {
        this.userId = userId;
        this.badge = badge;
        this.seasonId = seasonId;
    }
}
