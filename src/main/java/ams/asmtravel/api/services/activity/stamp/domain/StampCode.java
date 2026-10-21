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

/**
 * A code posted at a destination for check-ins, printed as a QR by the CMS. A place
 * may hold several active prints at once (the gate, a brochure, a partner's desk),
 * told apart by their labels; a code stops working only when deliberately retired,
 * so a leaked print is retired alone and the others keep going.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "engagement", name = "stamp_codes")
public class StampCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StampTarget targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 32, unique = true)
    private String code;

    /** Where this print is posted, so staff can tell prints apart; null when not said. */
    @Column(length = 80)
    private String label;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant issuedAt;

    /** The staff account that issued it; null for one issued by the system. */
    private UUID issuedBy;

    /** When it was deliberately retired; null while the code works. */
    private Instant retiredAt;

    /** The staff account that retired it; null while active. */
    private UUID retiredBy;

    public StampCode(StampTarget targetType, UUID targetId, String code, String label, UUID issuedBy) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.code = code;
        this.label = label;
        this.issuedBy = issuedBy;
    }

    public boolean active() {
        return retiredAt == null;
    }

    public void retire(UUID by) {
        this.retiredAt = Instant.now();
        this.retiredBy = by;
    }
}
