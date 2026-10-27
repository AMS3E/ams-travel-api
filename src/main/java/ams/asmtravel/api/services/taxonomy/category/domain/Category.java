package ams.asmtravel.api.services.taxonomy.category.domain;

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

/** What kind of place something is: a temple, a waterfall, a night market. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Its Khmer name, typed by hand; null until written. */
    @Column(length = 100)
    private String nameKm;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    /** An icon's name, not an upload. */
    @Column(length = 512)
    private String iconKey;

    /** Its uploaded icon picture (folder {@code category}); null until one is chosen. */
    @Column(length = 512)
    private String coverKey;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean isActive = true;

    /** Who made it, from their account; null for the ones the seed wrote. */
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
