package ams.asmtravel.api.services.corridor.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One day of a corridor's itinerary and its ordered stops. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "corridor_days",
        uniqueConstraints = @UniqueConstraint(name = "uk_corridor_days_number", columnNames = {"corridor_id", "day_number"}))
public class CorridorDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Counts from 1. */
    @Column(nullable = false)
    private Integer dayNumber;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    private LocalTime startTime;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corridor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Corridor corridor;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<CorridorStop> stops = new ArrayList<>();
}
