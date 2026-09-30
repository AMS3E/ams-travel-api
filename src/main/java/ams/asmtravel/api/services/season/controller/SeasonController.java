package ams.asmtravel.api.services.season.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.season.dto.request.CreateSeasonRequest;
import ams.asmtravel.api.services.season.dto.request.UpdateSeasonRequest;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import ams.asmtravel.api.services.season.dto.response.UserSeasonProgressResponse;
import ams.asmtravel.api.services.season.exception.SeasonNotFoundException;
import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.services.user.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

/** Tourist seasons (high and green), their lifecycle, and each person's season totals. */
@Tag(name = "Badges · Seasons", description = "The high and green seasons badge periods follow.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seasons")
public class SeasonController {

    private final SeasonService seasonService;
    private final UserService userService;

    @GetMapping
    public List<SeasonResponse> getAll() {
        return seasonService.getAll();
    }

    /** Own path so "current" is not read as an id. */
    @GetMapping("/current")
    public SeasonResponse getCurrent() {
        return seasonService.findCurrent()
                .orElseThrow(() -> new SeasonNotFoundException("No season is active"));
    }

    /** The caller's totals in a season: the active one, or {@code ?seasonId=}. */
    @GetMapping("/me/progress")
    public UserSeasonProgressResponse getMyProgress(Authentication authentication, @RequestParam(required = false) UUID seasonId) {
        return seasonService.findProgress(userService.requireUserId(authentication), seasonId);
    }

    /** The caller's totals in every season they took part in, newest first. */
    @GetMapping("/me/history")
    public List<UserSeasonProgressResponse> getMyHistory(Authentication authentication) {
        return seasonService.findHistory(userService.requireUserId(authentication));
    }

    @GetMapping("/{id}")
    public SeasonResponse getById(@PathVariable UUID id) {
        return seasonService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SeasonResponse create(@Valid @RequestBody CreateSeasonRequest request) {
        return seasonService.create(request);
    }

    /** Starts this season now; the one active before it is completed, its history kept. */
    @PostMapping("/{id}/activate")
    public SeasonResponse activate(@PathVariable UUID id) {
        return seasonService.activate(id);
    }

    @PostMapping("/{id}/complete")
    public SeasonResponse complete(@PathVariable UUID id) {
        return seasonService.complete(id);
    }

    @PutMapping("/{id}")
    public SeasonResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSeasonRequest request) {
        return seasonService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        seasonService.delete(id);
    }
}
