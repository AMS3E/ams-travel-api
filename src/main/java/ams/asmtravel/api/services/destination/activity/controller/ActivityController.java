package ams.asmtravel.api.services.destination.activity.controller;

import ams.asmtravel.api.services.destination.activity.dto.request.ActivityRequest;
import ams.asmtravel.api.services.destination.activity.dto.response.ActivityResponse;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** Activities and experiences. Reviews live at {@code /reviews?targetType=ACTIVITY}. */
@Tag(name = "Catalogue · Activities & Experiences", description = "Things to do: dives, kayak tours, treks, cruises.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final UserService userService;

    /** With {@code ?near=} a destination id: published activities near it, nearest first; otherwise all the caller may see. */
    @GetMapping
    public List<ActivityResponse> getAll(@RequestParam(required = false) UUID destinationId,
                                         @RequestParam(required = false) String slug,
                                         @RequestParam(required = false) UUID near) {
        return near != null ? activityService.getNear(near) : activityService.getAll(destinationId, slug);
    }

    @GetMapping("/{id}")
    public ActivityResponse getById(@PathVariable UUID id) {
        return activityService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ActivityResponse create(Authentication authentication, @Valid @RequestBody ActivityRequest request) {
        return activityService.create(request, userService.requireUserId(authentication));
    }

    @PutMapping("/{id}")
    public ActivityResponse update(@PathVariable UUID id, @Valid @RequestBody ActivityRequest request) {
        return activityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        activityService.delete(id);
    }
}
