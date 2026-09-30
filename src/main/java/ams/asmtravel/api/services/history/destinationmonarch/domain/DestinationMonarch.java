package ams.asmtravel.api.services.history.destinationmonarch.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

/** A monarch who built or shaped a destination, and how. The monarch is a plain id the database keys. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "history", name = "destination_monarchs")
public class DestinationMonarch {

    @EmbeddedId
    private DestinationMonarchId id = new DestinationMonarchId();

    @Column(columnDefinition = "text")
    private String description;

    @MapsId("destinationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Destination destination;

    public DestinationMonarch(Destination destination, UUID monarchId, String description) {
        this.id.setDestinationId(destination.getId());
        this.id.setMonarchId(monarchId);
        this.destination = destination;
        this.description = description;
    }
}
