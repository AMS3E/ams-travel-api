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
 * The code posted at a destination for check-ins, printed as a QR by the CMS.
 * One is current per place; issuing another replaces it, so a leaked print
 * stops working without touching the app.
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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant issuedAt;

    /** The staff account that issued it; null for one issued by the system. */
    private UUID issuedBy;

    /** When a newer code took its place; null while current. */
    private Instant replacedAt;

    public StampCode(StampTarget targetType, UUID targetId, String code, UUID issuedBy) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.code = code;
        this.issuedBy = issuedBy;
    }

    public boolean current() {
        return replacedAt == null;
    }
}
