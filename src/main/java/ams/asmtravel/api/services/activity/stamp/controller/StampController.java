package ams.asmtravel.api.services.activity.stamp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.activity.stamp.dto.request.StampRequest;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampResponse;
import ams.asmtravel.api.services.activity.stamp.service.StampService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Digital stamps: on-site check-in and collected stamps. Badge progress is {@code /badge-progress/me}. */
@Tag(name = "Engagement · Digital stamps", description = "Checking in on site, and the stamps collected.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stamps")
public class StampController {

    private final StampService stampService;
    private final UserService userService;

    /** Checks the caller in at a destination to collect its stamp. */
    @PostMapping
    public StampResponse collect(Authentication authentication, @Valid @RequestBody StampRequest request) {
        try {
            return stampService.collect(userService.requireUserId(authentication), request);
        } catch (DataIntegrityViolationException raced) {
            // Two check-ins raced to the same day's stamp; the other one got it.
            return stampService.alreadyCollected(request.destinationId());
        }
    }

    @GetMapping("/me")
    public List<StampResponse> findMine(Authentication authentication) {
        return stampService.findMine(userService.requireUserId(authentication));
    }
}
