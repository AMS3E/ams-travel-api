package ams.asmtravel.api.services.review.review.controller;

import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.review.review.dto.request.CreateReviewRequest;
import ams.asmtravel.api.services.review.review.dto.request.UpdateReviewRequest;
import ams.asmtravel.api.services.review.review.dto.response.ReviewResponse;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.service.ReviewService;
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

/** What customers wrote about places, and moderating it. */
@Tag(name = "Engagement · Reviews", description = "What customers wrote, and moderating it.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    /** A place's published reviews; moderators may filter by status and author. */
    @GetMapping
    public Page<ReviewResponse> getAll(@RequestParam(required = false) ReviewTargetType targetType,
                                       @RequestParam(required = false) UUID targetId,
                                       @RequestParam(required = false) ReviewStatus status,
                                       @RequestParam(required = false) UUID userId,
                                       @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                       Pageable pageable) {
        return reviewService.getAll(targetType, targetId, status, userId, pageable);
    }

    /** Review counts and ratings for a list page: {@code ?targetType=DESTINATION&targetIds=a,b}. */
    @GetMapping("/summary")
    public List<ReviewSummaryResponse> getSummaries(@RequestParam ReviewTargetType targetType,
                                                    @RequestParam List<UUID> targetIds) {
        return reviewService.summaries(targetType, targetIds);
    }

    @GetMapping("/me")
    public Page<ReviewResponse> findMine(Authentication authentication,
                                         @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                         Pageable pageable) {
        return reviewService.findMine(userService.requireUserId(authentication), pageable);
    }

    @GetMapping("/{id}")
    public ReviewResponse getById(@PathVariable UUID id) {
        return reviewService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReviewResponse create(Authentication authentication, @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(userService.requireUserId(authentication), request);
    }

    /** The author edits the words; a moderator sends {@code status} (and a note) to moderate. */
    @PutMapping("/{id}")
    public ReviewResponse update(Authentication authentication, @PathVariable UUID id,
                                 @Valid @RequestBody UpdateReviewRequest request) {
        return reviewService.update(id, request, userService.requireUserId(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        reviewService.delete(id, userService.requireUserId(authentication));
    }
}
