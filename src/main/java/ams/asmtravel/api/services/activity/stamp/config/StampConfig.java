package ams.asmtravel.api.services.activity.stamp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Reads {@code app.badges.*} into {@link StampProperties}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StampProperties.class)
public class StampConfig {
}
