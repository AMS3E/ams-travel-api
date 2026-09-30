package ams.asmtravel.api.services.destination.destination.domain;

import ams.asmtravel.api.services.history.architecture.domain.ArchitectureStyle;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import ams.asmtravel.api.services.history.religion.domain.Religion;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.region.domain.Region;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A place worth going to, such as Angkor Wat. Lives in a province and starts as a DRAFT. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "destination", name = "destinations")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(columnDefinition = "text")
    private String address;

    @Column(length = 512)
    private String coverKey;

    /** Main tourism region code; drives its form and page. See {@link DestinationThemes}. */
    @Column(length = 40)
    private String theme;

    /** Slugs from {@link DestinationThemes} for its theme. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> specialties = new ArrayList<>();

    /** Centuries 1 to 21, for the Ancient Capitals Century filter. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> centuries = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(nullable = false)
    private Boolean isFeatured = false;

    /** Distinct visitors a day, from browse events. */
    @Column(nullable = false)
    private Long viewCount = 0L;

    /** Author's {@code auth.users} id; null once deleted. */
    private UUID createdBy;

    private Instant publishedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_categories",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_tags",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_interests",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new HashSet<>();

    /** Tourism regions only; its zone comes from its province. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_regions",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "region_id"))
    private Set<Region> tourismRegions = new HashSet<>();

    /** Architecture styles, from the history schema. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "history", name = "destination_architecture_styles",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "architecture_style_id"))
    private Set<ArchitectureStyle> architectureStyles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "history", name = "destination_religions",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "religion_id"))
    private Set<Religion> religions = new HashSet<>();

    /** Historical periods, besides those its timeline names. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "history", name = "destination_periods",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "historical_period_id"))
    private Set<HistoricalPeriod> historicalPeriods = new HashSet<>();

    public boolean isPublished() {
        return status == ListingStatus.PUBLISHED;
    }
}
