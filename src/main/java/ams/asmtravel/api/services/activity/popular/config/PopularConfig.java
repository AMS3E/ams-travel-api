package ams.asmtravel.api.services.activity.popular.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Reads {@code app.popular.*} into {@link PopularProperties}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PopularProperties.class)
public class PopularConfig {
}
