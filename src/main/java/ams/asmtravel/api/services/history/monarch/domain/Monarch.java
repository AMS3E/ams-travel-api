package ams.asmtravel.api.services.history.monarch.domain;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A Khmer king who built or shaped the places in the catalogue. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "history", name = "monarchs")
public class Monarch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 150)
    private String title;

    private Integer reignStartYear;

    private Integer reignEndYear;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 512)
    private String imageKey;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
