package ams.asmtravel.api.services.history.period.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.history.period.dto.request.HistoricalPeriodRequest;
import ams.asmtravel.api.services.history.period.dto.response.HistoricalPeriodResponse;
import ams.asmtravel.api.services.history.period.service.HistoricalPeriodService;
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

/** The ages of Khmer history. */
@Tag(name = "Heritage · Historical periods", description = "The eras a site belongs to.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/historical-coverage/periods")
public class HistoricalPeriodController {

    private final HistoricalPeriodService periodService;

    @GetMapping
    public List<HistoricalPeriodResponse> getAll() {
        return periodService.getAll();
    }

    @GetMapping("/{id}")
    public HistoricalPeriodResponse getById(@PathVariable UUID id) {
        return periodService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public HistoricalPeriodResponse create(@Valid @RequestBody HistoricalPeriodRequest request) {
        return periodService.create(request);
    }

    @PutMapping("/{id}")
    public HistoricalPeriodResponse update(@PathVariable UUID id, @Valid @RequestBody HistoricalPeriodRequest request) {
        return periodService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        periodService.delete(id);
    }
}
