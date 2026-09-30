package ams.asmtravel.api.services.review.rating.controller;

import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.services.review.rating.dto.request.CreateRatingRequest;
import ams.asmtravel.api.services.review.rating.dto.request.UpdateRatingRequest;
import ams.asmtravel.api.services.review.rating.dto.response.RatingResponse;
import ams.asmtravel.api.services.review.rating.dto.response.RatingSummaryResponse;
import ams.asmtravel.api.services.review.rating.service.RatingService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Stars for destinations, stays and food listings; one rating per person per place. */
@Tag(name = "Engagement · Ratings", description = "Rate a place, and each place's average and star counts.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final UserService userService;

    /** A page of active ratings, newest first: {@code ?targetType=&targetId=&stars=&page=&size=&sort=}. */
    @GetMapping
    public Page<RatingResponse> getAll(@RequestParam(required = false) RatingTargetType targetType,
                                       @RequestParam(required = false) UUID targetId,
                                       @RequestParam(required = false) Integer stars,
                                       @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                       Pageable pageable) {
        return ratingService.getAll(targetType, targetId, stars, pageable);
    }

    /** Average, total and 1–5 counts for a list page: {@code ?targetType=DESTINATION&targetIds=a,b}. */
    @GetMapping("/summary")
    public List<RatingSummaryResponse> getSummaries(@RequestParam RatingTargetType targetType,
                                                    @RequestParam List<UUID> targetIds) {
        return ratingService.summaries(targetType, targetIds);
    }

    @GetMapping("/me")
    public RatingResponse findMine(Authentication authentication,
                                   @RequestParam RatingTargetType targetType,
                                   @RequestParam UUID targetId) {
        return ratingService.findMine(userService.requireUserId(authentication), targetType, targetId);
    }

    @GetMapping("/{id}")
    public RatingResponse getById(@PathVariable UUID id) {
        return ratingService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse create(Authentication authentication, @Valid @RequestBody CreateRatingRequest request) {
        return ratingService.create(userService.requireUserId(authentication), request);
    }

    @PutMapping("/{id}")
    public RatingResponse update(Authentication authentication, @PathVariable UUID id,
                                 @Valid @RequestBody UpdateRatingRequest request) {
        return ratingService.update(id, request, userService.requireUserId(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        ratingService.delete(id, userService.requireUserId(authentication));
    }
}
