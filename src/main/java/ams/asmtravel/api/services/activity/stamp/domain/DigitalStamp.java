package ams.asmtravel.api.services.activity.stamp.domain;

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

/** An on-site check-in: one per person, place and season. The place is a plain id, so stamps outlive it. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "engagement", name = "digital_stamps")
public class DigitalStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    /** Which kind of destination; the column keeps its old name. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StampTarget targetType = StampTarget.DESTINATION;

    @Column(nullable = false)
    private UUID destinationId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant collectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StampSource source = StampSource.QR;

    /** The code that was scanned, so the CMS counts check-ins per print; null for stamps older than the codes. */
    private UUID codeId;

    public DigitalStamp(UUID userId, StampTarget targetType, UUID destinationId, UUID codeId) {
        this.userId = userId;
        this.targetType = targetType;
        this.destinationId = destinationId;
        this.codeId = codeId;
    }
}
