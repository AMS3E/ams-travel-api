package ams.asmtravel.api.services.badge.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A badge definition: what it counts, how many earn it, and its level on the ladder. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "gamification", name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Upper case, unique ({@code REVIEWER_GOLDEN}). */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 512)
    private String iconKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BadgeType type;

    /** Null for a collection badge, which is on no ladder. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BadgeLevel level;

    /** How many of {@code type} in one season earn it. */
    @Column(nullable = false)
    private Integer threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BadgeStatus status = BadgeStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** For a collection badge: the destinations to stamp. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(schema = "gamification", name = "badge_destinations", joinColumns = @JoinColumn(name = "badge_id"))
    @Column(name = "destination_id", nullable = false)
    private Set<UUID> destinationIds = new HashSet<>();
}
