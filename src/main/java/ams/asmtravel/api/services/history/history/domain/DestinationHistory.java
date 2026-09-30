package ams.asmtravel.api.services.history.history.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A moment in a destination's past: when it was built, rebuilt, rediscovered. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "history", name = "destination_history")
public class DestinationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** A plain id, keyed by the database to the period service's table. */
    private UUID historicalPeriodId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    /** CE; null when only a century or a range is known. */
    private Integer eventYear;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(columnDefinition = "text")
    private String sourceReference;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Destination destination;
}
