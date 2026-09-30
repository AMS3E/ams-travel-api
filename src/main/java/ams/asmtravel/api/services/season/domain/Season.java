package ams.asmtravel.api.services.season.domain;

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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One season with its own dates (not a yearly rule); badge counts restart each season. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "gamification", name = "seasons")
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Stored in the {@code kind} column the calendar was first seeded with. */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private SeasonType type;

    /** Inclusive; badge periods are matched on this date. */
    @Column(nullable = false, unique = true)
    private LocalDate startsOn;

    /** Inclusive. */
    @Column(nullable = false)
    private LocalDate endsOn;

    /** Inactive seasons are never current and may overlap. */
    @Column(nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'UPCOMING'")
    private SeasonStatus status = SeasonStatus.UPCOMING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
