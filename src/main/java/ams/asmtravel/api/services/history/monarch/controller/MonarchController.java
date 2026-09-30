package ams.asmtravel.api.services.history.monarch.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.history.monarch.dto.request.MonarchRequest;
import ams.asmtravel.api.services.history.monarch.dto.response.MonarchResponse;
import ams.asmtravel.api.services.history.monarch.service.MonarchService;
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

/** The Khmer kings behind the places in the catalogue. */
@Tag(name = "Heritage · Kings", description = "The kings a site is tied to.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/historical-coverage/monarchs")
public class MonarchController {

    private final MonarchService monarchService;

    @GetMapping
    public List<MonarchResponse> getAll() {
        return monarchService.getAll();
    }

    @GetMapping("/{id}")
    public MonarchResponse getById(@PathVariable UUID id) {
        return monarchService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public MonarchResponse create(@Valid @RequestBody MonarchRequest request) {
        return monarchService.create(request);
    }

    @PutMapping("/{id}")
    public MonarchResponse update(@PathVariable UUID id, @Valid @RequestBody MonarchRequest request) {
        return monarchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        monarchService.delete(id);
    }
}
