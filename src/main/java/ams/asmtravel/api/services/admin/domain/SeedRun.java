package ams.asmtravel.api.services.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One row per seed reset that ran: the token named in {@code app.seed.reset-token} and when. A token
 * seen before never wipes again, so the variable can stay set after the deploy it was meant for.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(schema = "system", name = "seed_runs")
public class SeedRun {

    @Id
    @Column(length = 100)
    private String token;

    @Column(nullable = false)
    private Instant ranAt;

    public SeedRun(String token) {
        this.token = token;
        this.ranAt = Instant.now();
    }
}
