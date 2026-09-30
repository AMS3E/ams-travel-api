package ams.asmtravel.api.services.badge.controller;

import ams.asmtravel.api.services.badge.domain.BadgeStatus;
import ams.asmtravel.api.services.badge.domain.BadgeType;
import ams.asmtravel.api.services.badge.dto.request.CreateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.request.UpdateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.response.BadgeProgressResponse;
import ams.asmtravel.api.services.badge.dto.response.BadgeResponse;
import ams.asmtravel.api.services.badge.dto.response.UserBadgeResponse;
import ams.asmtravel.api.services.badge.service.BadgeService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/** Badge definitions, and each person's progress and unlocks per season. */
@Tag(name = "Badges", description = "Definitions, seasonal progress, level upgrades and earned badges.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/badges")
public class BadgeController {

    private final BadgeService badgeService;
    private final UserService userService;

    /** {@code ?type=&status=}; each may be left out. */
    @GetMapping
    public List<BadgeResponse> getAll(@RequestParam(required = false) BadgeType type,
                                      @RequestParam(required = false) BadgeStatus status) {
        return badgeService.getAll(type, status);
    }

    /** The caller's ladders and badges in a season: the active one, or {@code ?seasonId=}. */
    @GetMapping("/me/progress")
    public BadgeProgressResponse getMyProgress(Authentication authentication, @RequestParam(required = false) UUID seasonId) {
        return badgeService.findProgress(userService.requireUserId(authentication), seasonId);
    }

    /** Every badge the caller earned, newest first, across all seasons. */
    @GetMapping("/me/earned")
    public List<UserBadgeResponse> getMyEarned(Authentication authentication) {
        return badgeService.findEarned(userService.requireUserId(authentication));
    }

    /** The caller opened a destination: counted once per place per day towards the browse ladder. */
    @PutMapping("/me/explorations/{destinationId}")
    public BadgeProgressResponse explore(Authentication authentication, @PathVariable UUID destinationId) {
        return badgeService.explore(userService.requireUserId(authentication), destinationId);
    }

    /** One earned badge by its share link: public, for a share page. */
    @GetMapping("/shared/{userBadgeId}")
    public UserBadgeResponse getShared(@PathVariable UUID userBadgeId) {
        return badgeService.findShared(userBadgeId);
    }

    @GetMapping("/{id}")
    public BadgeResponse getById(@PathVariable UUID id) {
        return badgeService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BadgeResponse create(@Valid @RequestBody CreateBadgeRequest request) {
        return badgeService.create(request);
    }

    @PutMapping("/{id}")
    public BadgeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBadgeRequest request) {
        return badgeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        badgeService.delete(id);
    }
}
