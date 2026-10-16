package ams.asmtravel.api.services.taxonomy.interest.domain;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Why someone travels — historical sites, diving, Khmer cooking — and what they browse by. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "interests")
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    /** The Discover layer lists interests under this — Attraction Sites, Stays, Food. Null: not listed. */
    @Column(length = 60)
    private String groupName;

    /** An icon's name, not an upload; no longer edited, kept for clients that still draw it. */
    @Column(length = 512)
    private String iconKey;

    /** Its uploaded picture (folder {@code interest}); null until one is chosen. */
    @Column(length = 512)
    private String coverKey;

    /** Its place in its group, 0 for the first: the Explore shelf shows the group in this order. */
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean isActive = true;

    /** Who made it, from their account; null for the ones the seed wrote. */
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
