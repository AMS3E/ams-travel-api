package ams.asmtravel.api.services.badge.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code app.badges.share.*}: where a badge's share page is reached from outside.
 *
 * @param publicUrl the API's public origin ({@code https://api.travel.ams.com.kh}); the share page sits under it
 */
@Validated
@ConfigurationProperties(prefix = "app.badges.share")
public record BadgeProperties(@NotBlank String publicUrl) {

    public BadgeProperties {
        publicUrl = publicUrl == null || publicUrl.isBlank() ? "http://localhost:8080" : publicUrl.replaceAll("/+$", "");
    }

    /** The page Facebook and friends are sent to for one earned badge. */
    public String shareUrl(java.util.UUID userBadgeId) {
        return publicUrl + "/api/v1/badges/shared/" + userBadgeId + "/page";
    }
}
