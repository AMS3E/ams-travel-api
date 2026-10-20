package ams.asmtravel.api.services.search.mapper;

import ams.asmtravel.api.services.corridor.dto.response.CorridorResponse;
import ams.asmtravel.api.services.destination.destination.dto.response.DestinationResponse;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.search.domain.KhmerNames;
import ams.asmtravel.api.services.search.domain.SearchType;
import ams.asmtravel.api.services.search.dto.response.SearchHitResponse;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A place of any kind as one card. Ratings and verification come from their own services and may be null. */
@Mapper(componentModel = "spring")
public interface SearchMapper {

    default SearchHitResponse toHit(DestinationResponse place, Map<UUID, String> provinceNames, Map<UUID, String> categoryNames,
                                    ReviewSummaryResponse review, VerificationResponse verification) {
        List<UUID> categoryIds = ids(place.categoryIds());
        return new SearchHitResponse(SearchType.DESTINATION, place.id(), place.name(), place.slug(), place.shortDescription(),
                place.coverUrl(), place.provinceId(), provinceNames.get(place.provinceId()), place.latitude(), place.longitude(),
                categoryIds, names(categoryIds, categoryNames), ids(place.interestIds()), ids(place.tourismRegionIds()),
                review == null ? null : review.averageRating(), review == null ? 0 : review.ratingCount(),
                review == null ? 0 : review.reviewCount(),
                verification != null && verification.verified(),
                verification == null || verification.basis() == null ? null : verification.basis().name(),
                verification != null && Boolean.TRUE.equals(verification.travelerRecommended()),
                null, null, null, null, null, null, null, null, List.of());
    }

    default SearchHitResponse toHit(StayResponse stay, Map<UUID, String> provinceNames, ReviewSummaryResponse review) {
        return new SearchHitResponse(SearchType.STAY, stay.id(), stay.name(), null, stay.shortDescription(), stay.coverUrl(),
                stay.provinceId(), provinceNames.get(stay.provinceId()), stay.latitude(), stay.longitude(),
                List.of(), List.of(), ids(stay.interestIds()), List.of(),
                review == null ? null : review.averageRating(), review == null ? 0 : review.ratingCount(),
                review == null ? 0 : review.reviewCount(), false, null, false,
                stay.priceFrom(), stay.currency(), null, null, null, null, null, null, List.of());
    }

    default SearchHitResponse toHit(FoodResponse food, Map<UUID, String> provinceNames, Map<UUID, String> categoryNames,
                                    ReviewSummaryResponse review) {
        List<UUID> categoryIds = ids(food.categoryIds());
        return new SearchHitResponse(SearchType.FOOD, food.id(), food.name(), food.slug(), food.shortDescription(), food.coverUrl(),
                food.provinceId(), provinceNames.get(food.provinceId()), food.latitude(), food.longitude(),
                categoryIds, names(categoryIds, categoryNames), ids(food.interestIds()), List.of(),
                review == null ? null : review.averageRating(), review == null ? 0 : review.ratingCount(),
                review == null ? 0 : review.reviewCount(), false, null, false,
                food.priceFrom(), food.currency(), null, food.openNow(), food.todayHours(), null, null, null, List.of());
    }

    default SearchHitResponse toHit(CorridorResponse corridor, ReviewSummaryResponse review) {
        return new SearchHitResponse(SearchType.CORRIDOR, corridor.id(), corridor.name(), corridor.slug(), corridor.shortDescription(),
                corridor.coverUrl(), null, null, null, null,
                List.of(), List.of(), ids(corridor.interestIds()), List.of(),
                review == null ? null : review.averageRating(), review == null ? 0 : review.ratingCount(),
                review == null ? 0 : review.reviewCount(), false, null, false,
                null, null, corridor.durationDays(), null, null, corridor.stopNames(), corridor.encodedPolyline(), null, List.of());
    }

    /**
     * The card in Khmer: each word swapped where a Khmer one is written, the rest left in English and named in
     * {@code untranslated}. Corridors have no Khmer text of their own yet.
     */
    default SearchHitResponse inKhmer(SearchHitResponse hit, String nameKm, String summaryKm, KhmerNames names) {
        List<String> untranslated = new ArrayList<>();
        String name = pick(nameKm, hit.name(), "name", untranslated);
        String summary = pick(summaryKm, hit.shortDescription(), "shortDescription", untranslated);
        String province = hit.provinceId() == null ? hit.provinceName()
                : pick(names.provincesKm().get(hit.provinceId()), hit.provinceName(), "provinceName", untranslated);
        List<String> categories = new ArrayList<>();
        boolean englishCategory = false;
        for (UUID id : hit.categoryIds()) {
            String english = names.categories().get(id);
            String khmer = names.categoriesKm().get(id);
            if (khmer != null) {
                categories.add(khmer);
            } else if (english != null) {
                categories.add(english);
                englishCategory = true;
            }
        }
        if (englishCategory) {
            untranslated.add("categoryNames");
        }
        if (hit.stopNames() != null && !hit.stopNames().isEmpty()) {
            untranslated.add("stopNames");
        }
        return hit.withText(name, summary, province, categories, List.copyOf(untranslated));
    }

    /** The Khmer when written; else the English, noted as untranslated when there is any. */
    static String pick(String khmer, String english, String field, List<String> untranslated) {
        if (khmer != null && !khmer.isBlank()) {
            return khmer;
        }
        if (english != null && !english.isBlank()) {
            untranslated.add(field);
        }
        return english;
    }

    static List<UUID> ids(Collection<UUID> ids) {
        return ids == null ? List.of() : List.copyOf(ids);
    }

    /** The names of the ids that have one, in the same order. */
    static List<String> names(List<UUID> ids, Map<UUID, String> names) {
        List<String> found = new ArrayList<>();
        for (UUID id : ids) {
            String name = names.get(id);
            if (name != null) {
                found.add(name);
            }
        }
        return found;
    }
}
