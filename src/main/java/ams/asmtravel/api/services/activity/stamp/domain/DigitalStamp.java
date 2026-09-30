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

/** An on-site check-in: one per person, place and day. The place is a plain id, so stamps outlive it. */
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

    @Column(nullable = false)
    private UUID destinationId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant collectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StampSource source = StampSource.GPS;

    public DigitalStamp(UUID userId, UUID destinationId) {
        this.userId = userId;
        this.destinationId = destinationId;
    }
}
