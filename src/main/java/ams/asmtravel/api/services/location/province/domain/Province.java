package ams.asmtravel.api.services.location.province.domain;

import ams.asmtravel.api.services.location.region.domain.Region;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One of Cambodia's provinces, in a zone or in none. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "taxonomy", name = "provinces")
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    /** Its Khmer name, typed by hand; null until written. */
    @Column(length = 150)
    private String nameKm;

    /** Its address on the website: {@code siem-reap}, made from its name. */
    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 5)
    private String countryCode = "KH";

    @Column(nullable = false)
    private Boolean isActive = true;

    /** The picture on its card in the app's "Explore by Provinces" row; null until an editor chooses one. */
    @Column(length = 512)
    private String coverKey;

    /** Its centre, the marker the Map screen puts on it: the capital town, as seeded, or where an editor moved it. */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /** Who made it, from their account; null for the 25 the seed wrote. */
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Its zone, or null when none was chosen. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;
}
