package ams.asmtravel.api.services.activity.stamp.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code app.badges.*}: under the badges prefix because the Adventure badge counts stamps.
 *
 * @param stampRadiusMeters how close a visitor must stand to a destination to collect its stamp
 */
@Validated
@ConfigurationProperties(prefix = "app.badges")
public record StampProperties(@Min(1) Integer stampRadiusMeters) {

    public StampProperties {
        stampRadiusMeters = stampRadiusMeters == null ? 1000 : stampRadiusMeters;
    }
}
