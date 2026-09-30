package ams.asmtravel.api.services.activity.recommendation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Reads {@code app.recommendation.*} into {@link RecommendationProperties}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RecommendationProperties.class)
public class RecommendationConfig {
}
