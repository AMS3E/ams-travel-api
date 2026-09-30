package ams.asmtravel.api.services.user.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * {@code app.user.*}.
 *
 * @param lastLoginPrecision how stale {@code last_login_at} may get before it is written again
 */
@ConfigurationProperties(prefix = "app.user")
public record UserProperties(Duration lastLoginPrecision) {

    public UserProperties {
        lastLoginPrecision = lastLoginPrecision == null ? Duration.ofMinutes(10) : lastLoginPrecision;
    }
}
