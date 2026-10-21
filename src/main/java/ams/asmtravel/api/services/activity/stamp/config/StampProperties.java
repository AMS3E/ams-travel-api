package ams.asmtravel.api.services.activity.stamp.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code app.badges.*}: under the badges prefix because the Adventure badge counts stamps.
 *
 * @param stampRadiusMeters how close a visitor must stand to a destination to collect its stamp
 * @param share             the API's public origin, read from {@code app.badges.share.public-url} as the badge
 *                          share page does; a printed QR links to the scan page under it
 */
@Validated
@ConfigurationProperties(prefix = "app.badges")
public record StampProperties(@Min(1) Integer stampRadiusMeters, Share share) {

    public StampProperties {
        stampRadiusMeters = stampRadiusMeters == null ? 1000 : stampRadiusMeters;
        share = share == null ? new Share(null) : share;
    }

    public record Share(String publicUrl) {
        public Share {
            publicUrl = publicUrl == null || publicUrl.isBlank() ? "http://localhost:8080" : publicUrl.replaceAll("/+$", "");
        }
    }

    /** What a printed QR encodes: the scan page, whose last part is the code. */
    public String scanUrl(String code) {
        return share.publicUrl() + "/api/v1/stamps/scan/" + code;
    }
}
