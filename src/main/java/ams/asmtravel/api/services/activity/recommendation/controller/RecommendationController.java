package ams.asmtravel.api.services.activity.recommendation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.activity.recommendation.dto.request.RecommendationRequest;
import ams.asmtravel.api.services.activity.recommendation.dto.response.RecommendationResponse;
import ams.asmtravel.api.services.activity.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The homepage's Recommended list: personal when signed in, everyone's otherwise. */
@Tag(name = "Engagement · Recommendations", description = "What to suggest to one customer.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserService userService;

    @GetMapping("/for-you")
    public RecommendationResponse forYou(Authentication authentication,
                                         @Valid @ModelAttribute RecommendationRequest request) {
        return recommendationService.forVisitor(userService.findUserId(authentication).orElse(null), request);
    }
}
