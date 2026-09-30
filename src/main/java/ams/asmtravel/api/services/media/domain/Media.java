package ams.asmtravel.api.services.media.domain;

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

/** One photo of a destination, stay, food listing or review; the file itself is in MinIO. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "media", name = "media_items")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaType ownerType;

    /** No foreign key: the owner's deleting event removes its photos. */
    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 512)
    private String mediaKey;

    private String caption;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    /** One cover per owner; setting one clears the others. */
    @Column(nullable = false)
    private Boolean isCover = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaStatus status = MediaStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Media(MediaType ownerType, UUID ownerId, String mediaKey, int sortOrder) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.mediaKey = mediaKey;
        this.sortOrder = sortOrder;
    }
}
