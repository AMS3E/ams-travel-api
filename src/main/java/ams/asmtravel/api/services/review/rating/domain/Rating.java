package ams.asmtravel.api.services.review.rating.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** One person's stars for one place; no foreign key, the place's deleting event removes it. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "engagement", name = "ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_ratings_user_target",
                columnNames = {"user_id", "target_type", "target_id"}))
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RatingTargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    /** 1 to 5. */
    @Column(nullable = false)
    private Short stars;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RatingStatus status = RatingStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public Rating(UUID userId, RatingTargetType targetType, UUID targetId, int stars) {
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.stars = (short) stars;
    }
}
