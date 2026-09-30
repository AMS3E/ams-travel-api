package ams.asmtravel.api.services.destination.food.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.destination.food.dto.request.FoodRequest;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import ams.asmtravel.api.services.destination.food.service.FoodService;
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

/** Places to eat. Reviews live at {@code /foods/{id}/reviews}. */
@Tag(name = "Catalogue · Food listings", description = "Places to eat and drink.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/foods")
public class FoodController {

    private final FoodService foodService;
    private final UserService userService;

    /** With {@code ?near=} a destination id: published listings near it, nearest first; otherwise all the caller may see. */
    @GetMapping
    public List<FoodResponse> getAll(@RequestParam(required = false) UUID destinationId,
                                     @RequestParam(required = false) String slug,
                                     @RequestParam(required = false) UUID near) {
        return near != null ? foodService.getNear(near) : foodService.getAll(destinationId, slug);
    }

    @GetMapping("/{id}")
    public FoodResponse getById(@PathVariable UUID id) {
        return foodService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public FoodResponse create(Authentication authentication, @Valid @RequestBody FoodRequest request) {
        return foodService.create(request, userService.requireUserId(authentication));
    }

    @PutMapping("/{id}")
    public FoodResponse update(@PathVariable UUID id, @Valid @RequestBody FoodRequest request) {
        return foodService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        foodService.delete(id);
    }
}
