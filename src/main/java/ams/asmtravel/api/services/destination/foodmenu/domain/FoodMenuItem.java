package ams.asmtravel.api.services.destination.foodmenu.domain;

import ams.asmtravel.api.services.destination.food.domain.Food;
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

/** One dish on a food listing's menu, with price, spice level and a Khmer name to point at. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "destination", name = "destination_food_menu_items")
public class FoodMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    /** Khmer name, to point at when ordering. */
    @Column(length = 150)
    private String localName;

    /** How it sounds, e.g. "sach ko aing". */
    @Column(length = 150)
    private String pronunciation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuCourse course = MenuCourse.MAIN;

    @Column(columnDefinition = "text")
    private String description;

    /** In the listing's currency. */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    /** What the price buys, e.g. "One bowl". */
    @Column(length = 60)
    private String portion;

    /** People it feeds when shared. */
    @Column(nullable = false)
    private Integer serves = 1;

    /** 0 not spicy, 1 mild, 2 hot, 3 Khmer hot. */
    @Column(nullable = false)
    private Integer spiceLevel = 0;

    /** Slugs from {@code FoodVocabularies.DISH_LABELS}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> labels = new ArrayList<>();

    /** Photo keys from the listing's gallery; the first one leads. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> photoKeys = new ArrayList<>();

    /** A signature dish; always shown first. */
    @Column(nullable = false)
    private Boolean isSignature = false;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    /** False hides the dish from travellers. */
    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Food food;
}
