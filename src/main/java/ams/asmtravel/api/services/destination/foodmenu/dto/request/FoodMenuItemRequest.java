package ams.asmtravel.api.services.destination.foodmenu.dto.request;

import ams.asmtravel.api.services.destination.foodmenu.domain.MenuCourse;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A dish, for create and edit. Counts and flags are boxed so a missing one gets its default, not a 400.
 *
 * @param foodId     fixed once the dish exists
 * @param localName  the Khmer name
 * @param price      the cheapest dish sets the listing's "from" price
 * @param spiceLevel 0 not spicy to 3 Khmer hot
 * @param labels     slugs from {@code FoodVocabularies.DISH_LABELS}
 */
public record FoodMenuItemRequest(
        @NotNull UUID foodId,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String localName,
        @Size(max = 150) String pronunciation,
        @NotNull MenuCourse course,
        String description,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Size(max = 60) String portion,
        @Min(1) @Max(20) Integer serves,
        @Min(0) @Max(3) Integer spiceLevel,
        List<@NotBlank @Size(max = 100) String> labels,
        @Size(max = 6, message = "A dish shows at most six photos")
        List<@MediaKey(message = "Choose the photos from the listing's gallery") String> photoKeys,
        Boolean isSignature,
        Integer sortOrder,
        Boolean isActive
) {
}
