package ams.asmtravel.api.services.verification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Reads {@code app.verification.*} into {@link VerificationProperties}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VerificationProperties.class)
public class VerificationConfig {
}
