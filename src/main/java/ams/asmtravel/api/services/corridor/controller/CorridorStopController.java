package ams.asmtravel.api.services.corridor.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.corridor.dto.request.CorridorStopRequest;
import ams.asmtravel.api.services.corridor.dto.response.CorridorStopResponse;
import ams.asmtravel.api.services.corridor.service.CorridorItineraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** A corridor's stops as one flat, ordered list across all days. */
@Tag(name = "Catalogue · Travel route stops", description = "A route's destinations in the order they are visited.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/corridor-destinations")
public class CorridorStopController {

    private final CorridorItineraryService itineraryService;

    @GetMapping
    public List<CorridorStopResponse> getAll(@RequestParam(required = false) UUID corridorId) {
        return itineraryService.getStops(corridorId);
    }

    /** Replaces a one-day route; send every stop in order. */
    @PutMapping("/{corridorId}")
    public List<CorridorStopResponse> replace(@PathVariable UUID corridorId, @Valid @RequestBody CorridorStopRequest request) {
        itineraryService.replaceStops(corridorId, request);
        return itineraryService.getStops(corridorId);
    }
}
