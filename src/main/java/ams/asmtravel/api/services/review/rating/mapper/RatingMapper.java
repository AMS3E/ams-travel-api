package ams.asmtravel.api.services.review.rating.mapper;

import ams.asmtravel.api.services.review.rating.domain.Rating;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.services.review.rating.dto.response.RatingResponse;
import ams.asmtravel.api.services.review.rating.dto.response.RatingSummaryResponse;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    RatingResponse toResponse(Rating rating);

    /** A place's summary from its counts per number of stars; JSON keys are {@code "1"} to {@code "5"}, zeros included. */
    default RatingSummaryResponse toSummaryResponse(RatingTargetType targetType, UUID targetId, Map<Integer, Long> starCounts) {
        Map<String, Long> stars = new LinkedHashMap<>();
        long count = 0;
        long total = 0;
        for (int star = 1; star <= 5; star++) {
            long ratings = starCounts.getOrDefault(star, 0L);
            stars.put(String.valueOf(star), ratings);
            count += ratings;
            total += ratings * star;
        }
        BigDecimal average = count == 0 ? null
                : BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
        return new RatingSummaryResponse(targetType, targetId, average, count, stars);
    }
}
