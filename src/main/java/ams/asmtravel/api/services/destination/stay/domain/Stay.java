package ams.asmtravel.api.services.destination.stay.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.KhmerText;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Somewhere to sleep, found by its map pin near destinations. No slug: the website uses its id. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "destination", name = "destination_stays")
public class Stay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The {@code auth.users} id of the owner; null for an editorial listing. */
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StayType type;

    @Column(precision = 2, scale = 1)
    private BigDecimal starRating;

    /** One line for a card. */
    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    /** One line someone said about it, quoted at the top of its page on the app. */
    @Column(length = 300)
    private String slogan;

    /** Who said it. */
    @Column(length = 120)
    private String sloganBy;

    /** Khmer twins of the English text, typed by hand; null until written. */
    @Column(length = 255)
    private String nameKm;

    @Column(columnDefinition = "text")
    private String descriptionKm;

    @Column(length = 300)
    private String sloganKm;

    @Column(length = 120)
    private String sloganByKm;

    @Column(columnDefinition = "text")
    private String addressKm;

    /** The stay's policies in the host's own words, as Markdown: check-in and check-out, children, pets, smoking, paying. */
    @Column(columnDefinition = "text")
    private String policies;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceFrom;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> amenities = new ArrayList<>();

    /** The nights a room's weekend price applies to, named by the day they start; Friday and Saturday unless the host says otherwise. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> weekendNights = new ArrayList<>(List.of("FRIDAY", "SATURDAY"));

    /** Slugs from {@link StaySpecialties} for its kind of stay. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> specialties = new ArrayList<>();

    @Column(length = 512)
    private String coverKey;

    /** Local time, no date. */
    private LocalTime checkInFrom;

    private LocalTime checkOutUntil;

    /** Null when guests may arrive any time after {@code checkInFrom}. */
    private LocalTime checkInUntil;

    private LocalTime checkOutFrom;

    /** Minimum age to check in without an adult. */
    private Integer minCheckInAge;

    @Column(nullable = false)
    private Boolean childrenAllowed = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PetsPolicy petsPolicy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SmokingPolicy smokingPolicy;

    @Column(nullable = false)
    private Boolean partiesAllowed = false;

    /** "22:00-07:00". */
    @Column(length = 20)
    private String quietHours;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BreakfastOption breakfastOption;

    /** Slugs from {@link StayVocabularies#BREAKFAST_TYPES}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> breakfastTypes = new ArrayList<>();

    /** Per person, when breakfast costs extra. */
    @Column(precision = 12, scale = 2)
    private BigDecimal breakfastPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ParkingOption parkingOption;

    /** Per day, when parking is paid. */
    @Column(precision = 12, scale = 2)
    private BigDecimal parkingPrice;

    /** Slugs from {@link StayVocabularies#PAYMENT_METHODS}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> paymentMethods = new ArrayList<>();

    /** ISO 639-1 codes from {@link StayVocabularies#LANGUAGES}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> languagesSpoken = new ArrayList<>();

    @Column(length = 50)
    private String phone;

    private String contactEmail;

    @Column(length = 512)
    private String website;

    /** A handle ("@riverside") or a t.me link. */
    @Column(length = 100)
    private String telegram;

    @Column(length = 50)
    private String whatsapp;

    @Column(length = 512)
    private String facebookUrl;

    /** Ministry of Tourism licence. */
    @Column(length = 100)
    private String licenseNumber;

    private Integer yearOpened;

    @Column(columnDefinition = "text")
    private String address;

    /** The province the pin is in, when known. Travellers still find it by its pin. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingStatus status = ListingStatus.DRAFT;

    /** First opens of a day, signed in or not, all time: the popular service bumps it. */
    @Column(nullable = false)
    private Long viewCount = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Optional destination; deleting it keeps the stay. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Destination destination;

    /** Interests from the Stays group. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_stay_interests",
            joinColumns = @JoinColumn(name = "stay_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new HashSet<>();

    /** Filled English fields with no Khmer yet, by request name; publishing waits until this is empty. */
    public List<String> missingKhmer() {
        List<String> missing = new ArrayList<>();
        if (KhmerText.lacks(name, nameKm)) {
            missing.add("nameKm");
        }
        if (KhmerText.lacks(description, descriptionKm)) {
            missing.add("descriptionKm");
        }
        if (KhmerText.lacks(slogan, sloganKm)) {
            missing.add("sloganKm");
        }
        if (KhmerText.lacks(sloganBy, sloganByKm)) {
            missing.add("sloganByKm");
        }
        if (KhmerText.lacks(address, addressKm)) {
            missing.add("addressKm");
        }
        return missing;
    }
}
