package ams.asmtravel.api.services.corridor.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A stop on a corridor day: the destination, times, and the leg from the previous stop.
 * Place data is read from the destination, never copied; a destination may appear twice.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "destination", name = "corridor_destinations",
        uniqueConstraints = @UniqueConstraint(name = "uk_corridor_destinations_day_order",
                columnNames = {"corridor_day_id", "stop_order"}))
public class CorridorStop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Order within the day, from 1. */
    @Column(nullable = false)
    private Integer stopOrder;

    private LocalTime arrivalTime;

    private LocalTime departureTime;

    private Integer recommendedDurationMinutes;

    /** Transport from the previous stop; null on the first stop. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CorridorTransportType transportType;

    /** Optional visit title, shown above the destination name. */
    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String notes;

    /* Leg from the previous stop; null on the first stop and before routing. */

    @Column(precision = 10, scale = 2)
    private BigDecimal distanceFromPreviousKm;

    private Integer travelTimeFromPreviousMinutes;

    @Column(columnDefinition = "text")
    private String routePolyline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corridor_day_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CorridorDay day;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Destination destination;

    public CorridorLeg leg() {
        return new CorridorLeg(distanceFromPreviousKm, travelTimeFromPreviousMinutes, routePolyline);
    }

    public void setLeg(CorridorLeg leg) {
        distanceFromPreviousKm = leg == null ? null : leg.distanceKm();
        travelTimeFromPreviousMinutes = leg == null ? null : leg.minutes();
        routePolyline = leg == null ? null : leg.polyline();
    }

    public boolean hasLeg() {
        return distanceFromPreviousKm != null && travelTimeFromPreviousMinutes != null && routePolyline != null;
    }
}
