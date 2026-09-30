package ams.asmtravel.api.services.history.religion.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.history.religion.dto.request.ReligionRequest;
import ams.asmtravel.api.services.history.religion.dto.response.ReligionResponse;
import ams.asmtravel.api.services.history.religion.service.ReligionService;
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

/** The faiths the places in the catalogue were built for. */
@Tag(name = "Heritage · Religions", description = "The faiths a site was built for.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/historical-coverage/religions")
public class ReligionController {

    private final ReligionService religionService;

    @GetMapping
    public List<ReligionResponse> getAll() {
        return religionService.getAll();
    }

    @GetMapping("/{id}")
    public ReligionResponse getById(@PathVariable UUID id) {
        return religionService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReligionResponse create(@Valid @RequestBody ReligionRequest request) {
        return religionService.create(request);
    }

    @PutMapping("/{id}")
    public ReligionResponse update(@PathVariable UUID id, @Valid @RequestBody ReligionRequest request) {
        return religionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        religionService.delete(id);
    }
}
