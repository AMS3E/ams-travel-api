package ams.asmtravel.api.services.taxonomy.interest.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.taxonomy.interest.dto.request.InterestRequest;
import ams.asmtravel.api.services.taxonomy.interest.dto.response.InterestResponse;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Why people travel, and what the website browses by. */
@Tag(name = "Discover · Interests", description = "What travellers explore by, in their groups.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interests")
public class InterestController {

    private final InterestService interestService;

    @GetMapping
    public List<InterestResponse> getAll() {
        return interestService.getAll();
    }

    @GetMapping("/{id}")
    public InterestResponse getById(@PathVariable UUID id) {
        return interestService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public InterestResponse create(@Valid @RequestBody InterestRequest request) {
        return interestService.create(request);
    }

    @PutMapping("/{id}")
    public InterestResponse update(@PathVariable UUID id, @Valid @RequestBody InterestRequest request) {
        return interestService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        interestService.delete(id);
    }
}
