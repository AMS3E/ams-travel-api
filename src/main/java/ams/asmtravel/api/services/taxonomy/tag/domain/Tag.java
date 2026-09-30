package ams.asmtravel.api.services.taxonomy.tag.domain;

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

/** A free label: sunrise, elephant, fish amok. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true;

    /** Who made it, from their account; null for the ones the seed wrote. */
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
