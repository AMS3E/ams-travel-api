package ams.asmtravel.api.services.verification.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * One verification decision on a destination, with its evidence.
 * Append-only: the latest row says whether the place is verified.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "publishing", name = "destination_verifications")
public class DestinationVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** True verifies or renews; false revokes. */
    @Column(nullable = false)
    private Boolean verified;

    /** What the verification rests on; null on a revocation. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VerificationBasis basis;

    /** Invoice or receipt number. */
    @Column(length = 120)
    private String reference;

    /** Reason; required to revoke. */
    @Column(columnDefinition = "text")
    private String note;

    /** Key in the private verification folder; never public. */
    @Column(length = 512)
    private String evidenceKey;

    @Column(nullable = false)
    private UUID decidedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Destination destination;
}
