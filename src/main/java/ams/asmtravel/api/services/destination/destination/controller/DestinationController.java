package ams.asmtravel.api.services.destination.destination.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.destination.destination.dto.request.DestinationRequest;
import ams.asmtravel.api.services.destination.destination.dto.response.DestinationResponse;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

/**
 * Destinations. Slug is a {@code ?slug=} query, not a path, so sub-paths stay unambiguous.
 * No {@code @Validated}: Spring's method validation already returns 400 on {@link #findNearby}.
 */
@Tag(name = "Catalogue · Destinations", description = "Attractions: the catalogue's places, drafts and publishing.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/destinations")
public class DestinationController {

    private final DestinationService destinationService;
    private final UserService userService;

    @GetMapping
    public List<DestinationResponse> getAll(@RequestParam(required = false) String slug) {
        return destinationService.getPublished(slug);
    }

    @GetMapping("/nearby")
    public List<DestinationResponse> findNearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude,
            @RequestParam(defaultValue = "25") @DecimalMin("0.1") @DecimalMax("500") double radiusKm,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return destinationService.findNearby(latitude, longitude, radiusKm, limit);
    }

    /** Every place the caller's desk works on, in any status. */
    @GetMapping("/workspace")
    public List<DestinationResponse> getWorkspace() {
        return destinationService.getWorkspace();
    }

    @GetMapping("/{id}")
    public DestinationResponse getById(@PathVariable UUID id) {
        return destinationService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DestinationResponse create(Authentication authentication, @Valid @RequestBody DestinationRequest request) {
        return destinationService.create(request, userService.requireUserId(authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@destinationPermission.canEdit(#id, authentication)")
    public DestinationResponse update(@PathVariable UUID id, @Valid @RequestBody DestinationRequest request) {
        return destinationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@destinationPermission.canDelete(#id, authentication)")
    public void delete(@PathVariable UUID id) {
        destinationService.delete(id);
    }

    @PatchMapping("/{id}/publish")
    public DestinationResponse publish(@PathVariable UUID id) {
        return destinationService.publish(id);
    }

    @PatchMapping("/{id}/unpublish")
    public DestinationResponse unpublish(@PathVariable UUID id) {
        return destinationService.unpublish(id);
    }
}
