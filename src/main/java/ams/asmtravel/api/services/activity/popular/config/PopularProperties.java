package ams.asmtravel.api.services.activity.popular.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * {@code app.popular.*}.
 *
 * @param size how many places Popular shows
 * @param days how many days back it counts browses, today included
 * @param zone time zone that sets the day: a browse counts once per day in Cambodia
 */
@Validated
@ConfigurationProperties(prefix = "app.popular")
public record PopularProperties(
        @Min(1) Integer size,
        @Min(1) Integer days,
        ZoneId zone
) {
    public PopularProperties {
        size = size == null ? 10 : size;
        days = days == null ? 30 : days;
        zone = zone == null ? ZoneId.of("Asia/Phnom_Penh") : zone;
    }
}
