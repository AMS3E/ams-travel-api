package ams.asmtravel.api.services.location.region.domain;

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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A Discover region: a ZONE (made of provinces) or a TOURISM theme (shelved by
 * {@code categories}, filtered by {@code tags}, no provinces).
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    /** Its Khmer name, typed by hand; null until written. */
    @Column(length = 150)
    private String nameKm;

    /** Upper-case and short ({@code COAST}); the website's URL for the region is it lower-cased. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegionKind kind = RegionKind.ZONE;

    private UUID parentId;

    @Column(columnDefinition = "text")
    private String description;

    /** The description in Khmer; null until written. */
    @Column(columnDefinition = "text")
    private String descriptionKm;

    /** The picture on its card on Explore and the banner of its page in the app; null until one is chosen. */
    @Column(length = 512)
    private String coverKey;

    /** The picture in the Map panel of its page in the app, one image where the interactive map was; null until uploaded. */
    @Column(length = 512)
    private String mapKey;

    /** Its place among the regions of its kind, 0 for the first: Discover shows them in this order. */
    private Integer sortOrder;

    /** Its centre on the Map screen, when an editor set one; otherwise the response answers the middle of its places. */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * A tourism region's shelves, in order; empty on a zone.
     * Always replaced whole: link rows are keyed by region and category, so reordering in place would clash.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "taxonomy", name = "region_categories",
            joinColumns = @JoinColumn(name = "region_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @OrderColumn(name = "sort_order")
    private List<Category> categories = new ArrayList<>();

    /** The filter chips a tourism region offers, in its own order. Empty on a zone. Replaced whole, as the categories. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "taxonomy", name = "region_tags",
            joinColumns = @JoinColumn(name = "region_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @OrderColumn(name = "sort_order")
    private List<Tag> tags = new ArrayList<>();

    /** The interests an attraction of this theme is offered. Empty on a zone. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "taxonomy", name = "region_interests",
            joinColumns = @JoinColumn(name = "region_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new HashSet<>();
}
