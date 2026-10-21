package ams.asmtravel.api.services.destination.activity.dto.request;

import ams.asmtravel.api.services.destination.activity.domain.ActivityType;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * An activity or experience, for create and edit: an attraction's fields, its type, and how to reach
 * whoever runs it. On an edit, a null list keeps the links and an empty one clears them.
 *
 * @param destinationId    optional: the attraction it is run at or from
 * @param ownerUserId      a partner may name only themselves, an editor anyone
 * @param slug             blank: made from the name on create, kept on edit
 * @param slogan           one line someone said about it, quoted on its page; sloganBy says who
 * @param provinceId       required: the zone follows from it
 * @param tourismRegionIds zones are refused
 * @param status           null keeps it (new is DRAFT); only an editor may set PUBLISHED
 * @param nameKm           Khmer twins of the English text: null keeps what is stored, blank clears it
 */
public record ActivityRequest(
        UUID destinationId,
        UUID ownerUserId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 150) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        @NotNull ActivityType type,
        @Size(max = 500) String shortDescription,
        String description,
        @Size(max = 300) String slogan,
        @Size(max = 120) String sloganBy,
        @NotNull UUID provinceId,
        String address,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 255) String website,
        @Size(max = 100) String telegram,
        @Size(max = 50) String whatsapp,
        @Size(max = 512) String facebookUrl,
        @MediaKey(allowEmpty = true, message = "Upload the cover first, then send its key")
        String coverKey,
        ListingStatus status,
        Set<UUID> categoryIds,
        Set<UUID> tagIds,
        Set<UUID> interestIds,
        Set<UUID> tourismRegionIds,
        @Size(max = 255) String nameKm,
        String descriptionKm,
        @Size(max = 300) String sloganKm,
        @Size(max = 120) String sloganByKm,
        String addressKm
) {
}
