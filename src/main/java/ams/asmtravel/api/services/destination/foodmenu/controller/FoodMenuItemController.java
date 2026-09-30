package ams.asmtravel.api.services.destination.foodmenu.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.destination.foodmenu.dto.request.FoodMenuItemRequest;
import ams.asmtravel.api.services.destination.foodmenu.dto.response.FoodMenuItemResponse;
import ams.asmtravel.api.services.destination.foodmenu.service.FoodMenuItemService;
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

/** The dishes on a food listing's menu. */
@Tag(name = "Catalogue · Food menu", description = "The dishes and drinks one food listing serves.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/food-menu-items")
public class FoodMenuItemController {

    private final FoodMenuItemService menuService;

    @GetMapping
    public List<FoodMenuItemResponse> getAll(@RequestParam(required = false) UUID foodId) {
        return menuService.getAll(foodId);
    }

    @GetMapping("/{id}")
    public FoodMenuItemResponse getById(@PathVariable UUID id) {
        return menuService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public FoodMenuItemResponse create(@Valid @RequestBody FoodMenuItemRequest request) {
        return menuService.create(request);
    }

    @PutMapping("/{id}")
    public FoodMenuItemResponse update(@PathVariable UUID id, @Valid @RequestBody FoodMenuItemRequest request) {
        return menuService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        menuService.delete(id);
    }
}
