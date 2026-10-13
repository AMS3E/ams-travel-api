package ams.asmtravel.api.services.taxonomy.amenity.controller;

import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityLevel;
import ams.asmtravel.api.services.taxonomy.amenity.dto.request.AmenityRequest;
import ams.asmtravel.api.services.taxonomy.amenity.dto.response.AmenityResponse;
import ams.asmtravel.api.services.taxonomy.amenity.service.AmenityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/** What a stay offers, at the property and in the room: the one list both pickers read. */
@Tag(name = "Discover · Amenities", description = "What a stay offers — at the property, in the room, or both.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/amenities")
public class AmenityController {

    private final AmenityService amenityService;

    /** Every amenity, group by group; {@code ?level=PROPERTY} or {@code ROOM} keeps the ones offered there. */
    @GetMapping
    public List<AmenityResponse> getAll(@RequestParam(required = false) AmenityLevel level) {
        return amenityService.getAll(level);
    }

    @GetMapping("/{id}")
    public AmenityResponse getById(@PathVariable UUID id) {
        return amenityService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public AmenityResponse create(@Valid @RequestBody AmenityRequest request) {
        return amenityService.create(request);
    }

    @PutMapping("/{id}")
    public AmenityResponse update(@PathVariable UUID id, @Valid @RequestBody AmenityRequest request) {
        return amenityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        amenityService.delete(id);
    }
}
