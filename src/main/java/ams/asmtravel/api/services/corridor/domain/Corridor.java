package ams.asmtravel.api.services.corridor.domain;

import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A tourism corridor: a multi-day travel plan whose days hold ordered stops.
 * The road route is stored here so page views never call the routing service.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "corridors")
public class Corridor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    /** Its Khmer name, typed by hand; null until written. */
    @Column(length = 200)
    private String nameKm;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 500)
    private String shortDescription;

    /** The short description in Khmer; null until written. */
    @Column(length = 500)
    private String shortDescriptionKm;

    @Column(columnDefinition = "text")
    private String description;

    /** The description in Khmer; null until written. */
    @Column(columnDefinition = "text")
    private String descriptionKm;

    @Column(length = 512)
    private String coverKey;

    /** List position, 0 first; website and dashboard use this order. */
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CorridorStatus status = CorridorStatus.DRAFT;

    /** Derived from the days. */
    private Integer durationDays;

    private Integer durationNights;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalDistanceKm;

    /** Travel time only, without time at stops. */
    private Integer totalDurationMinutes;

    /** Cost per person, in US dollars. */
    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedBudgetMin;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedBudgetMax;

    /** "What you expect" on its page: short labels typed by the editor, in the editor's order. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> expectations = new ArrayList<>();

    /** First and last stop's destination; derived from the itinerary. */
    private UUID startDestinationId;

    private UUID endDestinationId;

    /** The whole road route as a Google-encoded line. */
    @Column(columnDefinition = "text")
    private String encodedPolyline;

    /** Signature of the stops the stored route was made for. */
    @Column(length = 64)
    private String routeSignature;

    /** Last route failure; null after a success. */
    @Column(length = 500)
    private String routeError;

    private Instant routeCalculatedAt;

    private Instant routeFailedAt;

    private Instant publishedAt;

    /** Creator's {@code auth.users} id; null once the user is deleted. */
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "corridor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<CorridorDay> days = new ArrayList<>();

    /** Tourism Corridors interests; each interest page lists the corridor. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "taxonomy", name = "corridor_interests",
            joinColumns = @JoinColumn(name = "corridor_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new HashSet<>();

    public boolean isPublished() {
        return status == CorridorStatus.PUBLISHED;
    }

    /** Sets days, nights, start and end from the itinerary. */
    public void summarise(List<CorridorStop> stopsInOrder) {
        int dayCount = days.size();
        durationDays = dayCount == 0 ? null : dayCount;
        durationNights = dayCount == 0 ? null : dayCount - 1;
        startDestinationId = stopsInOrder.isEmpty() ? null : stopsInOrder.getFirst().getDestination().getId();
        endDestinationId = stopsInOrder.isEmpty() ? null : stopsInOrder.getLast().getDestination().getId();
    }

    /** Stores the whole route and totals built from the stops' legs. */
    public void useRoute(List<CorridorStop> stopsInOrder, String signature, Instant calculatedAt) {
        BigDecimal distance = BigDecimal.ZERO;
        int minutes = 0;
        List<String> lines = new ArrayList<>();
        for (CorridorStop stop : stopsInOrder.subList(1, stopsInOrder.size())) {
            distance = distance.add(stop.getDistanceFromPreviousKm());
            minutes += stop.getTravelTimeFromPreviousMinutes();
            lines.add(stop.getRoutePolyline());
        }
        totalDistanceKm = distance.setScale(2, RoundingMode.HALF_UP);
        totalDurationMinutes = minutes;
        encodedPolyline = EncodedPolyline.join(lines);
        routeSignature = signature;
        routeCalculatedAt = calculatedAt;
        routeError = null;
        routeFailedAt = null;
    }

    /** Drops the stored route (its stops are gone). */
    public void clearRoute() {
        encodedPolyline = null;
        totalDistanceKm = null;
        totalDurationMinutes = null;
        routeSignature = null;
        routeCalculatedAt = null;
    }

    /** Records a route failure for the CMS; keeps any stored route. */
    public void routeFailed(String reason, Instant failedAt) {
        String text = reason == null || reason.isBlank() ? "The route could not be worked out." : reason;
        routeError = text.length() > 500 ? text.substring(0, 500) : text;
        routeFailedAt = failedAt;
    }
}
