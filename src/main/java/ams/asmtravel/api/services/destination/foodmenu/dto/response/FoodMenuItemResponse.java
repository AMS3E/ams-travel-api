package ams.asmtravel.api.services.destination.foodmenu.dto.response;

import ams.asmtravel.api.services.destination.foodmenu.domain.MenuCourse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param currency  the listing's currency, used by {@code price}
 * @param photoUrls links for {@code photoKeys}, in the same order
 */
public record FoodMenuItemResponse(
        UUID id,
        UUID foodId,
        String name,
        String localName,
        String pronunciation,
        MenuCourse course,
        String section,
        String description,
        BigDecimal price,
        String currency,
        String portion,
        Integer serves,
        Integer spiceLevel,
        List<String> labels,
        List<String> photoKeys,
        List<String> photoUrls,
        Boolean isSignature,
        Integer sortOrder,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
