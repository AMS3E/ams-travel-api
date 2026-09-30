package ams.asmtravel.api.services.authorization.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @NotEmpty List<String> allowedOriginPatterns,
        @NotEmpty List<String> allowedMethods,
        @NotEmpty List<String> allowedHeaders,
        List<String> exposedHeaders,
        boolean allowCredentials,
        Duration maxAge
) {
    public CorsProperties {
        exposedHeaders = exposedHeaders == null ? List.of() : List.copyOf(exposedHeaders);
        maxAge = maxAge == null ? Duration.ofHours(1) : maxAge;
    }
}
