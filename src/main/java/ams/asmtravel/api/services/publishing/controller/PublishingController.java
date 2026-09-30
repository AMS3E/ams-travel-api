package ams.asmtravel.api.services.publishing.controller;

import ams.asmtravel.api.services.publishing.domain.PublishingStatus;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import ams.asmtravel.api.services.publishing.dto.request.ApprovePublishRequest;
import ams.asmtravel.api.services.publishing.dto.request.RejectPublishRequest;
import ams.asmtravel.api.services.publishing.dto.request.SubmitPublishRequest;
import ams.asmtravel.api.services.publishing.dto.response.PublishingHistoryResponse;
import ams.asmtravel.api.services.publishing.dto.response.PublishingResponse;
import ams.asmtravel.api.services.publishing.service.PublishingService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Putting listings forward, answering them, and taking them live or off the site. */
@Tag(name = "Publishing", description = "Submit, approve or reject, publish and unpublish listings.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/publish-requests")
public class PublishingController {

    private final PublishingService publishingService;
    private final UserService userService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public PublishingResponse submit(Authentication authentication, @Valid @RequestBody SubmitPublishRequest request) {
        return publishingService.submit(request, userService.requireUserId(authentication));
    }

    @PostMapping("/{id}/approve")
    public PublishingResponse approve(Authentication authentication, @PathVariable UUID id,
                                      @Valid @RequestBody(required = false) ApprovePublishRequest request) {
        return publishingService.approve(id, request, userService.requireUserId(authentication));
    }

    @PostMapping("/{id}/reject")
    public PublishingResponse reject(Authentication authentication, @PathVariable UUID id,
                                     @Valid @RequestBody RejectPublishRequest request) {
        return publishingService.reject(id, request, userService.requireUserId(authentication));
    }

    @PostMapping("/{id}/publish")
    public PublishingResponse publish(Authentication authentication, @PathVariable UUID id) {
        return publishingService.publish(id, userService.requireUserId(authentication));
    }

    @PostMapping("/{id}/unpublish")
    public PublishingResponse unpublish(Authentication authentication, @PathVariable UUID id) {
        return publishingService.unpublish(id, userService.requireUserId(authentication));
    }

    /** The caller's desks' requests, decided ones too; {@code ?status=} narrows it. */
    @GetMapping
    public List<PublishingResponse> getAll(@RequestParam(required = false) PublishingStatus status) {
        return publishingService.findAll(status);
    }

    /** The caller's desks' requests waiting for a decision or to be published. */
    @GetMapping("/pending")
    public List<PublishingResponse> getPending() {
        return publishingService.findPending();
    }

    /** What the caller has put forward, and where each stands. */
    @GetMapping("/mine")
    public List<PublishingResponse> getMine(Authentication authentication) {
        UUID callerId = userService.requireUserId(authentication);
        return publishingService.findByCreator(callerId, callerId);
    }

    /** What one person put forward; full control only, unless it is the caller. */
    @GetMapping("/creators/{creatorId}")
    public List<PublishingResponse> getByCreator(Authentication authentication, @PathVariable UUID creatorId) {
        return publishingService.findByCreator(creatorId, userService.requireUserId(authentication));
    }

    /** A listing's latest request: {@code ?targetType=STAY&targetId=}. */
    @GetMapping("/current")
    public PublishingResponse getCurrent(Authentication authentication, @RequestParam PublishingTargetType targetType,
                                         @RequestParam UUID targetId) {
        return publishingService.findCurrent(targetType, targetId, userService.requireUserId(authentication));
    }

    /** A listing's publishing history, newest first: {@code ?targetType=STAY&targetId=}. */
    @GetMapping("/history")
    public List<PublishingHistoryResponse> getHistory(Authentication authentication,
                                                      @RequestParam PublishingTargetType targetType,
                                                      @RequestParam UUID targetId) {
        return publishingService.findHistory(targetType, targetId, userService.requireUserId(authentication));
    }

    @GetMapping("/{id}")
    public PublishingResponse getById(Authentication authentication, @PathVariable UUID id) {
        return publishingService.findById(id, userService.requireUserId(authentication));
    }
}
