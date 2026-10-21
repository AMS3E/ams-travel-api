package ams.asmtravel.api.services.verification.domain;

import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
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
 * One verification decision on a destination of any kind, with its evidence.
 * Append-only: the latest row says whether the place is verified. The place
 * is a plain id, so decisions outlive it.
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

    /** Which kind of destination; the id column keeps its old name. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublishingTargetType targetType = PublishingTargetType.DESTINATION;

    @Column(name = "destination_id", nullable = false)
    private UUID targetId;

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
}
