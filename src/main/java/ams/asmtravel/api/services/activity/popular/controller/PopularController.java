package ams.asmtravel.api.services.activity.popular.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.activity.popular.dto.request.BrowseEventRequest;
import ams.asmtravel.api.services.activity.popular.dto.response.PopularResponse;
import ams.asmtravel.api.services.activity.popular.service.PopularService;
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

import java.util.Set;
import java.util.UUID;

/** Browse events and the homepage's Popular list. A signed-in visitor is read from the token. */
@Tag(name = "Engagement · Popular", description = "What visitors browse, and the list made from it.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/popular")
public class PopularController {

    private final PopularService popularService;
    private final UserService userService;

    /** {@code ?regionId=} for one zone or tourism region, {@code ?provinceId=} for one province, {@code ?interestIds=} for any of some interests, {@code ?limit=} for how many. */
    @GetMapping
    public PopularResponse popular(@RequestParam(required = false) UUID regionId,
                                   @RequestParam(required = false) UUID provinceId,
                                   @RequestParam(required = false) Set<UUID> interestIds,
                                   @RequestParam(required = false) @Min(1) @Max(50) Integer limit) {
        return popularService.popular(regionId, provinceId, interestIds, limit);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void record(Authentication authentication, @Valid @RequestBody BrowseEventRequest request) {
        popularService.record(request, userService.findUserId(authentication).orElse(null));
    }
}
