package ams.asmtravel.api.services.activity.recommendation.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** {@code app.recommendation.*}: {@code size} is how many places Recommended shows. */
@Validated
@ConfigurationProperties(prefix = "app.recommendation")
public record RecommendationProperties(@Min(1) Integer size) {

    public RecommendationProperties {
        size = size == null ? 10 : size;
    }
}
