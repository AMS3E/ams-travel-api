package ams.asmtravel.api.services.history.destinationmonarch.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.history.destinationmonarch.dto.request.DestinationMonarchRequest;
import ams.asmtravel.api.services.history.destinationmonarch.dto.response.DestinationMonarchResponse;
import ams.asmtravel.api.services.history.destinationmonarch.service.DestinationMonarchService;
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

/** The monarchs who built or shaped each destination. */
@Tag(name = "Heritage · Destination kings", description = "Which kings a destination is tied to.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/destination-monarchs")
public class DestinationMonarchController {

    private final DestinationMonarchService linkService;

    @GetMapping
    public List<DestinationMonarchResponse> getAll(@RequestParam(required = false) UUID destinationId) {
        return linkService.getAll(destinationId);
    }

    /** Replaces the place's monarchs: send every one, with how each shaped it. */
    @PutMapping("/{destinationId}")
    public List<DestinationMonarchResponse> replace(@PathVariable UUID destinationId,
                                                    @Valid @RequestBody DestinationMonarchRequest request) {
        return linkService.replace(destinationId, request);
    }
}
