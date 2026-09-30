package ams.asmtravel.api.services.destination.stay.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.destination.stay.dto.request.StayRequest;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;
import ams.asmtravel.api.services.destination.stay.service.StayService;
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

/** Places to stay. Reviews live at {@code /stays/{id}/reviews}. */
@Tag(name = "Catalogue · Stays", description = "Places to sleep, written by their owner or the back office.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stays")
public class StayController {

    private final StayService stayService;

    /** With {@code ?near=} a destination id: published stays near it, nearest first; otherwise all the caller may see. */
    @GetMapping
    public List<StayResponse> getAll(@RequestParam(required = false) UUID destinationId,
                                     @RequestParam(required = false) UUID near) {
        return near != null ? stayService.getNear(near) : stayService.getAll(destinationId);
    }

    @GetMapping("/{id}")
    public StayResponse getById(@PathVariable UUID id) {
        return stayService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public StayResponse create(@Valid @RequestBody StayRequest request) {
        return stayService.create(request);
    }

    @PutMapping("/{id}")
    public StayResponse update(@PathVariable UUID id, @Valid @RequestBody StayRequest request) {
        return stayService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        stayService.delete(id);
    }
}
