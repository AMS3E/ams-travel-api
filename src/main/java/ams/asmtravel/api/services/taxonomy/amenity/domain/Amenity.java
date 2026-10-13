package ams.asmtravel.api.services.taxonomy.amenity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One thing a stay offers — a pool, free Wi-Fi, a hairdryer — kept as a
 * taxonomy the team manages. A stay and a room each store the slugs of
 * theirs; the level says which of the two lists offers it.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "amenities")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    /** The heading it is picked under: Internet, Bathroom, Food & drink… */
    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    /** An icon name the apps draw it with; null draws a plain tick. */
    @Column(name = "icon_key", length = 60)
    private String iconKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmenityLevel level = AmenityLevel.BOTH;

    /** Its place within its group, 0 first. */
    @Column(nullable = false)
    private Integer sortOrder = 0;

    /** Retired rather than deleted once stays carry it: off the pickers, still understood. */
    @Column(nullable = false)
    private Boolean isActive = true;

    /** Who made it, from their account; null for the ones the changelog wrote. */
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
