package ams.asmtravel.api.services.point.controller;

import ams.asmtravel.api.services.point.dto.request.PointHistoryRequest;
import ams.asmtravel.api.services.point.dto.response.PointTransactionResponse;
import ams.asmtravel.api.services.point.service.PointService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The points a person earned for rating and reviewing places. */
@Tag(name = "Points", description = "One point per place per day in the active season, for a rating or a review.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointService pointService;
    private final UserService userService;

    /** Newest first; {@code ?seasonId=} for one season. */
    @GetMapping("/me")
    public Page<PointTransactionResponse> findMine(Authentication authentication,
                                                   @ModelAttribute PointHistoryRequest request,
                                                   @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                                   Pageable pageable) {
        return pointService.findMine(userService.requireUserId(authentication), request, pageable);
    }
}
