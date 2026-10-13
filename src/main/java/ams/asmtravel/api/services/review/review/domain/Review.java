package ams.asmtravel.api.services.review.review.domain;

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
import java.time.LocalDate;
import java.util.UUID;

/** A visitor's review of a destination, stay or food, named by type and id. One per person per place. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "engagement", name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_reviews_user_target",
                columnNames = {"user_id", "target_type", "target_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewTargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewStatus status = ReviewStatus.PUBLISHED;

    /** Who last changed the status as a moderator, when, and why. */
    private UUID moderatedBy;

    private Instant moderatedAt;

    @Column(length = 500)
    private String moderationNote;

    /** Who the author travelled with, when they said. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReviewVisitType visitType;

    /** The month of the visit, kept as its first day; null when not said. */
    private LocalDate visitedOn;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
