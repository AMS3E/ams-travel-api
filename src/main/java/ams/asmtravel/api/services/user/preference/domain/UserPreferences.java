package ams.asmtravel.api.services.user.preference.domain;

import ams.asmtravel.api.services.user.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A person's site settings; one row per person, made on their first save.
 * {@code preferredRegionId} is a plain id: regions belong to the taxonomy service.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "auth", name = "user_preferences")
public class UserPreferences {

    public static final String DEFAULT_LANGUAGE = "en";

    public static final String DEFAULT_CURRENCY = "USD";

    @Id
    private UUID userId;

    @Column(nullable = false, length = 10)
    private String preferredLanguage = DEFAULT_LANGUAGE;

    @Column(nullable = false, length = 10)
    private String preferredCurrency = DEFAULT_CURRENCY;

    private UUID preferredRegionId;

    @Column(nullable = false)
    private boolean notificationEmail = true;

    @Column(nullable = false)
    private boolean notificationPush = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Theme theme = Theme.SYSTEM;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    public UserPreferences(User user) {
        this.user = user;
    }
}
