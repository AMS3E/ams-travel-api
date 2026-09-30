package ams.asmtravel.api.services.history.regioncoverage.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.location.region.domain.Region;
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
import java.util.UUID;

/**
 * One step in a region's succession: a royal capital, its years and the place to visit.
 * {@code dateRange} is free text ("c. 600 – 700 CE" is not a pair of years).
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "history", name = "region_coverage")
public class RegionCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 150)
    private String title;

    /** The same name in Khmer script, shown beside it. */
    @Column(length = 150)
    private String titleKm;

    @Column(length = 100)
    private String dateRange;

    /** A plain id, keyed by the database to the historical periods table. */
    private UUID historicalPeriodId;

    @Column(columnDefinition = "text")
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Region region;

    /** Null when the capital has no place in the catalogue to open. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private Destination destination;
}
