package ams.asmtravel.api.services.destination.stayroom.domain;

import ams.asmtravel.api.services.destination.stay.domain.Stay;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One kind of room a stay offers, and how many of it. Beds are counts per bed type, not free text. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "destination", name = "destination_stay_rooms")
public class StayRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    /** One line that sells the room. */
    @Column(length = 150)
    private String highlight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomType roomType;

    /** How many rooms of this kind the property has. */
    @Column(nullable = false)
    private Integer roomCount = 1;

    @Column(nullable = false)
    private Integer singleBeds = 0;

    @Column(nullable = false)
    private Integer doubleBeds = 0;

    @Column(nullable = false)
    private Integer queenBeds = 0;

    @Column(nullable = false)
    private Integer kingBeds = 0;

    /** A bunk bed sleeps two. */
    @Column(nullable = false)
    private Integer bunkBeds = 0;

    @Column(nullable = false)
    private Integer sofaBeds = 0;

    @Column(nullable = false)
    private Integer futonBeds = 0;

    @Column(nullable = false)
    private Integer maxAdults = 2;

    @Column(nullable = false)
    private Integer maxChildren = 0;

    @Column(precision = 6, scale = 1)
    private BigDecimal sizeSqm;

    /** A studio is one bedroom and no living room. */
    @Column(nullable = false)
    private Integer bedrooms = 1;

    @Column(nullable = false)
    private Integer livingRooms = 0;

    @Column(nullable = false)
    private Integer bathroomCount = 1;

    /** Ground floor is 0; null when it varies. */
    private Integer floor;

    @Column(nullable = false)
    private Boolean liftAccess = false;

    /** Null when the property has not said. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoomHousekeeping housekeeping;

    /** Null when there is no view worth naming. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RoomView roomView;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomBathroom bathroom = RoomBathroom.PRIVATE;

    /** Slugs from {@link RoomAmenities}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> amenities = new ArrayList<>();

    @Column(nullable = false)
    private Boolean smokingAllowed = false;

    @Column(nullable = false)
    private Boolean hasWindow = true;

    @Column(nullable = false)
    private Integer extraBeds = 0;

    @Column(precision = 12, scale = 2)
    private BigDecimal extraBedPrice;

    @Column(nullable = false)
    private Integer cots = 0;

    @Column(precision = 12, scale = 2)
    private BigDecimal cotPrice;

    /** Children under this age stay free in existing beds; null when none do. */
    private Integer childrenFreeUnderAge;

    /** In the stay's currency. */
    @Column(precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    @Column(nullable = false)
    private Boolean breakfastIncluded = false;

    /** Night price from November to April; null when it is the same all year. */
    @Column(precision = 12, scale = 2)
    private BigDecimal highSeasonPrice;

    /** Whether the price includes VAT and accommodation tax. */
    @Column(nullable = false)
    private Boolean taxesIncluded = true;

    @Column(nullable = false)
    private Integer minNights = 1;

    @Column(precision = 12, scale = 2)
    private BigDecimal weeklyPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    /** Deposit as a percent of the price. */
    private Integer depositPercent;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoomPaymentTerms paymentTerms;

    /** Free cancellation days before arrival; null is non-refundable, 0 means up to the day itself. */
    private Integer freeCancellationDays;

    @Column(columnDefinition = "text")
    private String description;

    /** Photo keys from the stay's gallery; the first one leads. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> photoKeys = new ArrayList<>();

    @Column(nullable = false)
    private Integer sortOrder = 0;

    /** False hides the room from the website but keeps it in the dashboard. */
    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stay_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Stay stay;

    /** People the beds sleep: single, sofa and futon beds one each, the rest two. */
    public int sleeps() {
        return singleBeds + sofaBeds + futonBeds + 2 * (doubleBeds + queenBeds + kingBeds + bunkBeds);
    }

    public int bedCount() {
        return singleBeds + doubleBeds + queenBeds + kingBeds + bunkBeds + sofaBeds + futonBeds;
    }
}
