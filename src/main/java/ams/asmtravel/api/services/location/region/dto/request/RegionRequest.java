package ams.asmtravel.api.services.location.region.dto.request;

import ams.asmtravel.api.services.media.dto.request.MediaKey;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @param code         such as {@code COASTAL-ISLAND}; null makes one from the name (new) or keeps it (edit)
 * @param kind         null keeps it (new: ZONE)
 * @param historyIntro the line under its Historical coverage heading
 * @param coverKey     the uploaded picture's key; null keeps it, empty removes it
 * @param mapKey       its map picture's key, the same way
 * @param categoryIds  a tourism region's shelves, in order; null keeps them
 * @param tagIds       its filter chips, in order; null keeps them
 * @param isActive     null keeps it (new: active)
 * @param amsRecommended Recommended by AMS Travel; null keeps it (new: not recommended)
 * @param sortOrder    its place within its kind, 0 first; null puts a new one last and keeps an edited one in place
 */
public record RegionRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 20) @Pattern(regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$",
                message = "Use letters and digits, words joined by hyphens") String code,
        RegionKind kind,
        UUID parentId,
        String description,
        String historyIntro,
        @MediaKey(allowEmpty = true, message = "Upload the picture first, then send its key") String coverKey,
        @MediaKey(allowEmpty = true, message = "Upload the map picture first, then send its key") String mapKey,
        List<UUID> categoryIds,
        List<UUID> tagIds,
        List<UUID> interestIds,
        Boolean isActive,
        Boolean amsRecommended,
        @PositiveOrZero Integer sortOrder,
        /** Its centre on the Map screen, with {@code longitude}: both or neither; neither keeps the current one. */
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude
) {
}
