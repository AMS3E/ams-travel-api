package ams.asmtravel.api.services.taxonomy.interestgroup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** One of the six groups the Interests tab lists interests under, with the picture shown for it. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "interest_groups")
public class InterestGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The name interests carry in their {@code groupName}: Attraction Sites, Stays, Food. */
    @Column(nullable = false, unique = true, length = 60)
    private String name;

    /** Its picture's key; null when it has none. */
    @Column(length = 512)
    private String coverKey;

    /** Its place in the row, 0 for the first. */
    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
