package ams.asmtravel.api.services.user.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Reads {@code app.user.*} into {@link UserProperties}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserProperties.class)
public class UserConfig {
}
