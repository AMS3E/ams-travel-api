package ams.asmtravel.api.services.favorite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A place a visitor saved: a destination, a stay, a food listing or a corridor. Once each. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "engagement", name = "favorites")
public class Favorite {

    @EmbeddedId
    private FavoriteId id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Favorite(UUID userId, FavoriteTarget targetType, UUID targetId) {
        this.id = new FavoriteId(userId, targetType, targetId);
    }
}
