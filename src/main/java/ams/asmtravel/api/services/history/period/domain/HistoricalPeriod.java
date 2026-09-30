package ams.asmtravel.api.services.history.period.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** An age of Khmer history — Pre-Angkor, Angkor, Post-Angkor — that styles and timelines belong to. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "history", name = "historical_periods")
public class HistoricalPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    /** Null when it has no clear beginning. */
    private Integer startYear;

    /** Null when it runs to the present. */
    private Integer endYear;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 512)
    private String coverKey;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
