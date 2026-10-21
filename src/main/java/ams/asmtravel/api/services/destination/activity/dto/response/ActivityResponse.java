package ams.asmtravel.api.services.destination.activity.dto.response;

import ams.asmtravel.api.services.destination.activity.domain.ActivityType;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID destinationId,
        UUID ownerUserId,
        String name,
        String slug,
        ActivityType type,
        String shortDescription,
        String description,
        String slogan,
        String sloganBy,
        UUID provinceId,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        String contactEmail,
        String website,
        String telegram,
        String whatsapp,
        String facebookUrl,
        String coverKey,
        String coverUrl,
        ListingStatus status,
        /** First opens of a day, signed in or not, all time. */
        Long viewCount,
        UUID createdBy,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<UUID> categoryIds,
        List<UUID> tagIds,
        List<UUID> interestIds,
        List<UUID> tourismRegionIds,
        /* Khmer twins of the English text, and those still empty while their English is filled. */
        String nameKm,
        String descriptionKm,
        String sloganKm,
        String sloganByKm,
        String addressKm,
        List<String> missingKhmer
) {
}
