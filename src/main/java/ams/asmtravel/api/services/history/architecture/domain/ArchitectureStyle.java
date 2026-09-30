package ams.asmtravel.api.services.history.architecture.domain;

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

/** A Khmer building style (Bakheng, Angkor Wat, Bayon) and the period it belongs to. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "history", name = "architecture_styles")
public class ArchitectureStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private UUID periodId;

    @Column(length = 512)
    private String imageKey;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
