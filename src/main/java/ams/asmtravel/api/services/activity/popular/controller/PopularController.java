package ams.asmtravel.api.services.activity.popular.controller;

import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import ams.asmtravel.api.services.activity.popular.dto.request.BrowseEventRequest;
import ams.asmtravel.api.services.activity.popular.dto.response.PopularResponse;
import ams.asmtravel.api.services.activity.popular.service.PopularService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Browse events and the app's Popular lists. A signed-in visitor is read from the token. */
@Tag(name = "Engagement · Popular", description = "What visitors browse, and the lists made from it.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/popular")
public class PopularController {

    private final PopularService popularService;
    private final UserService userService;

    /**
     * {@code ?type=DESTINATION|STAY|FOOD|ACTIVITY|CORRIDOR} (destinations when left out); {@code ?regionIds=} and
     * {@code ?provinceIds=} for any of several ({@code regionId} and {@code provinceId} still take one);
     * {@code ?interestIds=} for any of some interests; {@code ?limit=} for how many, 10 when left out.
     */
    @GetMapping
    public PopularResponse popular(@RequestParam(required = false) BrowseTarget type,
                                   @RequestParam(required = false) UUID regionId,
                                   @RequestParam(required = false) Set<UUID> regionIds,
                                   @RequestParam(required = false) UUID provinceId,
                                   @RequestParam(required = false) Set<UUID> provinceIds,
                                   @RequestParam(required = false) Set<UUID> interestIds,
                                   @RequestParam(required = false) @Min(1) @Max(50) Integer limit) {
        return popularService.popular(type, together(regionId, regionIds), together(provinceId, provinceIds), interestIds, limit);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void record(Authentication authentication, @Valid @RequestBody BrowseEventRequest request) {
        popularService.record(request, userService.findUserId(authentication).orElse(null));
    }

    private static Set<UUID> together(UUID one, Set<UUID> several) {
        Set<UUID> all = new HashSet<>();
        if (several != null) {
            all.addAll(several);
        }
        if (one != null) {
            all.add(one);
        }
        return all;
    }
}
