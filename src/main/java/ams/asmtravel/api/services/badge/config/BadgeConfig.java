package ams.asmtravel.api.services.badge.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Reads {@code app.badges.share.*} into {@link BadgeProperties}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BadgeProperties.class)
public class BadgeConfig {
}
