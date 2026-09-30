package ams.asmtravel.api.services.history.destinationmonarch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** The key of {@link DestinationMonarch}. */
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class DestinationMonarchId implements Serializable {

    @Column(name = "destination_id")
    private UUID destinationId;

    @Column(name = "monarch_id")
    private UUID monarchId;
}
