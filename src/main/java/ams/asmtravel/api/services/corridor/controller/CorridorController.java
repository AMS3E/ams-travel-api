package ams.asmtravel.api.services.corridor.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.corridor.dto.request.CorridorItineraryRequest;
import ams.asmtravel.api.services.corridor.dto.request.CorridorRequest;
import ams.asmtravel.api.services.corridor.dto.request.CorridorRoutePreviewRequest;
import ams.asmtravel.api.services.corridor.dto.response.CorridorItineraryResponse;
import ams.asmtravel.api.services.corridor.dto.response.CorridorResponse;
import ams.asmtravel.api.services.corridor.dto.response.CorridorRoutePreviewResponse;
import ams.asmtravel.api.services.corridor.service.CorridorItineraryService;
import ams.asmtravel.api.services.corridor.service.CorridorRouteService;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.user.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Tourism corridors: overview, publishing, day-by-day itinerary and road route. */
@Tag(name = "Catalogue · Travel routes", description = "Tourism corridors, their days and their publishing.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/corridors")
public class CorridorController {

    private final CorridorService corridorService;
    private final CorridorItineraryService itineraryService;
    private final CorridorRouteService routeService;
    private final UserService userService;

    /* The corridor. */

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CorridorResponse create(Authentication authentication, @Valid @RequestBody CorridorRequest request) {
        return corridorService.create(request, authorOf(authentication));
    }
    /** Published corridors (all for a curator); {@code ?slug=} finds one. */
    @GetMapping
    public List<CorridorResponse> getAll(@RequestParam(required = false) String slug) {
        return corridorService.getAll(slug);
    }

    @GetMapping("/{id}")
    public CorridorResponse getById(@PathVariable UUID id) {
        return corridorService.findById(id);
    }


    @PutMapping("/{id}")
    public CorridorResponse update(@PathVariable UUID id, @Valid @RequestBody CorridorRequest request) {
        return corridorService.update(id, request);
    }

    @PatchMapping("/{id}/publish")
    public CorridorResponse publish(@PathVariable UUID id) {
        corridorService.publish(id);
        return corridorService.findById(id);
    }

    @PatchMapping("/{id}/unpublish")
    public CorridorResponse unpublish(@PathVariable UUID id) {
        corridorService.unpublish(id);
        return corridorService.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        corridorService.delete(id);
    }

    /* Itinerary and route. */

    /** Days, stops and the road between them. */
    @GetMapping("/{id}/itinerary")
    public CorridorItineraryResponse getItinerary(@PathVariable UUID id) {
        return itineraryService.getItinerary(id);
    }

    /** Replaces days and stops; returns the itinerary after the route is updated. */
    @PutMapping("/{id}/itinerary")
    public CorridorItineraryResponse replaceItinerary(@PathVariable UUID id, @Valid @RequestBody CorridorItineraryRequest request) {
        itineraryService.replaceItinerary(id, request);
        return itineraryService.getItinerary(id);
    }

    /** Recalculates the road route now. */
    @PostMapping("/{id}/route")
    public CorridorItineraryResponse recalculateRoute(@PathVariable UUID id) {
        routeService.recalculateRoute(id);
        return itineraryService.getItinerary(id);
    }

    /** Roads between unsaved stops, for the planning map. Stores nothing. */
    @PostMapping("/route-preview")
    public CorridorRoutePreviewResponse previewRoute(@Valid @RequestBody CorridorRoutePreviewRequest request) {
        return routeService.previewRoute(request);
    }

    /** The author from the token, or null (creation never fails on this). */
    private UUID authorOf(Authentication authentication) {
        try {
            return userService.findUserId(authentication).orElse(null);
        } catch (ResponseStatusException unknown) {
            return null;
        }
    }
}
