package ams.asmtravel.api.services.history.religion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** A faith a place was built for or is still used by. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "history", name = "religions")
public class Religion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /** An icon's name, not an upload. */
    @Column(length = 512)
    private String iconKey;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
