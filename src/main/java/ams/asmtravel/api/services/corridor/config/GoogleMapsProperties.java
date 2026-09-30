package ams.asmtravel.api.services.corridor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * {@code google.maps.*}: server key for Google Routes ({@code GOOGLE_MAPS_API_KEY}); never sent to a browser.
 *
 * @param apiKey    empty turns Google routing off
 * @param routesUrl the {@code computeRoutes} endpoint
 * @param timeout   time limit for one request
 */
@ConfigurationProperties(prefix = "google.maps")
public record GoogleMapsProperties(String apiKey, URI routesUrl, Duration timeout) {

    public GoogleMapsProperties {
        apiKey = apiKey == null ? "" : apiKey.trim();
        routesUrl = routesUrl == null ? URI.create("https://routes.googleapis.com/directions/v2:computeRoutes") : routesUrl;
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
    }

    public boolean configured() {
        return !apiKey.isEmpty();
    }
}
