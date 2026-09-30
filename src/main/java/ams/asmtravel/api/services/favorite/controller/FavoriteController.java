package ams.asmtravel.api.services.favorite.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.favorite.domain.FavoriteTarget;
import ams.asmtravel.api.services.favorite.dto.request.FavoriteRequest;
import ams.asmtravel.api.services.favorite.dto.response.FavoriteResponse;
import ams.asmtravel.api.services.favorite.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The signed-in visitor's saved places. */
@Tag(name = "Engagement · Favourites", description = "What a customer saved.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;

    @GetMapping
    public List<FavoriteResponse> mine(Authentication authentication) {
        return favoriteService.findMine(userService.requireUserId(authentication));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public FavoriteResponse add(Authentication authentication, @Valid @RequestBody FavoriteRequest request) {
        return favoriteService.add(userService.requireUserId(authentication), request);
    }

    @DeleteMapping("/{targetType}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(Authentication authentication, @PathVariable FavoriteTarget targetType, @PathVariable UUID targetId) {
        favoriteService.remove(userService.requireUserId(authentication), targetType, targetId);
    }
}
