package ams.asmtravel.api.services.user.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Reads {@code app.admin.*} into {@link StaffProperties}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StaffProperties.class)
public class StaffConfig {
}
