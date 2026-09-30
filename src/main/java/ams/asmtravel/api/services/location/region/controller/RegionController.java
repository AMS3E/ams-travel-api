package ams.asmtravel.api.services.location.region.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.location.region.dto.request.RegionRequest;
import ams.asmtravel.api.services.location.region.dto.response.RegionResponse;
import ams.asmtravel.api.services.location.region.service.RegionService;
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

/** Discover regions: tourism zones (with their provinces) and themed tourism regions. */
@Tag(name = "Discover · Regions", description = "The five zones and the themed tourism regions.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public List<RegionResponse> getAll(@RequestParam(required = false) RegionKind kind) {
        return regionService.getAll(kind);
    }

    @GetMapping("/{id}")
    public RegionResponse getById(@PathVariable UUID id) {
        return regionService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public RegionResponse create(@Valid @RequestBody RegionRequest request) {
        return regionService.create(request);
    }

    @PutMapping("/{id}")
    public RegionResponse update(@PathVariable UUID id, @Valid @RequestBody RegionRequest request) {
        return regionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        regionService.delete(id);
    }
}
