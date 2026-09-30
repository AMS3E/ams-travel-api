package ams.asmtravel.api.services.history.architecture.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.history.architecture.dto.request.ArchitectureStyleRequest;
import ams.asmtravel.api.services.history.architecture.dto.response.ArchitectureStyleResponse;
import ams.asmtravel.api.services.history.architecture.service.ArchitectureStyleService;
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

/** The styles of Khmer building. */
@Tag(name = "Heritage · Architecture styles", description = "Building styles an ancient site is built in.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/historical-coverage/styles")
public class ArchitectureStyleController {

    private final ArchitectureStyleService styleService;

    @GetMapping
    public List<ArchitectureStyleResponse> getAll(@RequestParam(required = false) UUID periodId) {
        return styleService.getAll(periodId);
    }

    @GetMapping("/{id}")
    public ArchitectureStyleResponse getById(@PathVariable UUID id) {
        return styleService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ArchitectureStyleResponse create(@Valid @RequestBody ArchitectureStyleRequest request) {
        return styleService.create(request);
    }

    @PutMapping("/{id}")
    public ArchitectureStyleResponse update(@PathVariable UUID id, @Valid @RequestBody ArchitectureStyleRequest request) {
        return styleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        styleService.delete(id);
    }
}
