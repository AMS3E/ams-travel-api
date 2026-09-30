package ams.asmtravel.api.services.activity.recommendation.mapper;

import ams.asmtravel.api.services.activity.recommendation.domain.RankedDestination;
import ams.asmtravel.api.services.activity.recommendation.dto.response.RecommendationResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    RecommendationResponse.Place toPlace(RankedDestination destination);

    default RecommendationResponse toResponse(boolean personal, List<RankedDestination> ranked) {
        return new RecommendationResponse(personal, ranked.stream().map(this::toPlace).toList());
    }
}
