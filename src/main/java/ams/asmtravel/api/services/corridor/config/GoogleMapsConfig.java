package ams.asmtravel.api.services.corridor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@link GoogleMapsProperties} and {@link OsmRoutingProperties} (OSM is used when no Google key is set). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GoogleMapsProperties.class, OsmRoutingProperties.class})
public class GoogleMapsConfig {
}
