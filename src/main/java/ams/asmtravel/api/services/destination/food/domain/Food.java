package ams.asmtravel.api.services.destination.food.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Somewhere to eat or drink, found by its map pin near destinations. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "destination", name = "destination_foods")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FoodType type;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /** {@code $} to {@code $$$$}. */
    @Column(length = 20)
    private String priceRange;

    /** Usual spend per person for a meal. */
    @Column(precision = 12, scale = 2)
    private BigDecimal averageMealPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceFrom;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    /** Day ({@code mon}…{@code sun}) to hours ({@code 07:00-22:00}). */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> openingHours = new LinkedHashMap<>();

    @Column(length = 50)
    private String phone;

    private String contactEmail;

    private String website;

    /** Slugs from {@link FoodVocabularies#CUISINES}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> cuisines = new ArrayList<>();

    /** Slugs from {@link FoodVocabularies#MEALS}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> mealsServed = new ArrayList<>();

    /** Slugs from {@link FoodVocabularies#DIETARY}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> dietary = new ArrayList<>();

    /** Slugs from {@link FoodVocabularies#SERVICE_OPTIONS}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> serviceOptions = new ArrayList<>();

    /** Slugs from {@link FoodAmenities}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> amenities = new ArrayList<>();

    /** Slugs from {@link FoodSpecialties} for its kind of place. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> specialties = new ArrayList<>();

    /** Slugs from {@link FoodVocabularies#GOOD_FOR}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> goodFor = new ArrayList<>();

    /** Menu languages, ISO 639-1. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> menuLanguages = new ArrayList<>();

    /** Slugs from {@link FoodVocabularies#DELIVERY_PARTNERS}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> deliveryPartners = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> paymentMethods = new ArrayList<>();

    /** Staff languages, ISO 639-1. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> languagesSpoken = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AlcoholOption alcohol;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FoodParking parkingOption;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReservationPolicy reservations;

    /** Seats; null when unknown. */
    private Integer seatingCapacity;

    @Column(length = 100)
    private String telegram;

    @Column(length = 50)
    private String whatsapp;

    @Column(length = 512)
    private String facebookUrl;

    private Integer yearOpened;

    @Column(length = 512)
    private String coverKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(nullable = false)
    private Boolean isFeatured = false;

    @Column(nullable = false)
    private Boolean isLocalSpecialty = false;

    /** Average of visible reviews, kept by engagement; null when none. */
    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    private UUID createdBy;

    private Instant publishedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Optional destination; deleting it keeps the listing. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Destination destination;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_food_categories",
            joinColumns = @JoinColumn(name = "food_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_food_tags",
            joinColumns = @JoinColumn(name = "food_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_food_interests",
            joinColumns = @JoinColumn(name = "food_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new HashSet<>();
}
