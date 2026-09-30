package ams.asmtravel.api.services.history.regioncoverage.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.history.regioncoverage.dto.request.RegionCoverageRequest;
import ams.asmtravel.api.services.history.regioncoverage.dto.response.RegionCoverageResponse;
import ams.asmtravel.api.services.history.regioncoverage.service.RegionCoverageService;
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

/** The succession a region is read as, in order. */
@Tag(name = "Heritage · Regional coverage", description = "A region's history, told step by step.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/historical-coverage/regions")
public class RegionCoverageController {

    private final RegionCoverageService coverageService;

    @GetMapping
    public List<RegionCoverageResponse> getAll(@RequestParam(required = false) UUID regionId) {
        return coverageService.getAll(regionId);
    }

    @GetMapping("/{id}")
    public RegionCoverageResponse getById(@PathVariable UUID id) {
        return coverageService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegionCoverageResponse create(@Valid @RequestBody RegionCoverageRequest request) {
        return coverageService.create(request);
    }

    @PutMapping("/{id}")
    public RegionCoverageResponse update(@PathVariable UUID id, @Valid @RequestBody RegionCoverageRequest request) {
        return coverageService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        coverageService.delete(id);
    }
}
