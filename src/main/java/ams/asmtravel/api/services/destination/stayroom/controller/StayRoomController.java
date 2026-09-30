package ams.asmtravel.api.services.destination.stayroom.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.destination.stayroom.dto.request.StayRoomRequest;
import ams.asmtravel.api.services.destination.stayroom.dto.response.StayRoomResponse;
import ams.asmtravel.api.services.destination.stayroom.service.StayRoomService;
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

/** The rooms a stay offers. */
@Tag(name = "Catalogue · Stay rooms", description = "The rooms one stay offers, with their beds and prices.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stay-rooms")
public class StayRoomController {

    private final StayRoomService roomService;

    @GetMapping
    public List<StayRoomResponse> getAll(@RequestParam(required = false) UUID stayId) {
        return roomService.getAll(stayId);
    }

    @GetMapping("/{id}")
    public StayRoomResponse getById(@PathVariable UUID id) {
        return roomService.findById(id);
    }

    /** Refused unless at least one bed count is set. */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public StayRoomResponse create(@Valid @RequestBody StayRoomRequest request) {
        return roomService.create(request);
    }

    @PutMapping("/{id}")
    public StayRoomResponse update(@PathVariable UUID id, @Valid @RequestBody StayRoomRequest request) {
        return roomService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        roomService.delete(id);
    }
}
