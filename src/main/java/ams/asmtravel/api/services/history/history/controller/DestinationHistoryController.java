package ams.asmtravel.api.services.history.history.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.history.history.dto.request.DestinationHistoryRequest;
import ams.asmtravel.api.services.history.history.dto.response.DestinationHistoryResponse;
import ams.asmtravel.api.services.history.history.service.DestinationHistoryService;
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

/** A destination's timeline entries. Its monarchs are at {@code /destination-monarchs}. */
@Tag(name = "Heritage · Destination timeline", description = "One destination's history, a moment at a time.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/destination-histories")
public class DestinationHistoryController {

    private final DestinationHistoryService historyService;

    /** One place's timeline, or every entry the caller may see. */
    @GetMapping
    public List<DestinationHistoryResponse> getAll(@RequestParam(required = false) UUID destinationId) {
        return destinationId == null ? historyService.getAll() : historyService.getTimeline(destinationId);
    }

    @GetMapping("/{id}")
    public DestinationHistoryResponse getById(@PathVariable UUID id) {
        return historyService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DestinationHistoryResponse create(@Valid @RequestBody DestinationHistoryRequest request) {
        return historyService.create(request);
    }

    @PutMapping("/{id}")
    public DestinationHistoryResponse update(@PathVariable UUID id, @Valid @RequestBody DestinationHistoryRequest request) {
        return historyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        historyService.delete(id);
    }
}
