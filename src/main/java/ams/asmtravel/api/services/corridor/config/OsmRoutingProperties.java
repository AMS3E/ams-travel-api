package ams.asmtravel.api.services.corridor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * {@code osm.routing.*}: OSRM server (FOSSGIS layout: {@code routed-car/bike/foot}) used when no Google key is set.
 *
 * @param url       server without trailing slash; empty turns it off
 * @param timeout   time limit for one request
 * @param userAgent required by the public server to identify the caller
 */
@ConfigurationProperties(prefix = "osm.routing")
public record OsmRoutingProperties(String url, Duration timeout, String userAgent) {

    public OsmRoutingProperties {
        url = url == null ? "" : url.trim().replaceAll("/+$", "");
        timeout = timeout == null ? Duration.ofSeconds(15) : timeout;
        userAgent = userAgent == null || userAgent.isBlank() ? "AMS-Travel/1.0 (tourism corridor routes)" : userAgent.trim();
    }

    public boolean configured() {
        return !url.isEmpty();
    }
}
