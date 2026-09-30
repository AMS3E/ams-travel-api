package ams.asmtravel.api.services.user.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.dto.request.UserRequest;
import ams.asmtravel.api.services.user.user.dto.response.AuthorResponse;
import ams.asmtravel.api.services.user.user.dto.response.UserResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** All users for the dashboard, and the signed-in person's own profile under {@code /me}. */
@Tag(name = "People · Users", description = "Everyone with an account, customers and staff alike.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.createNew(request);
    }

    /** Back-office accounts as id and name only, to show who wrote a row. */
    @GetMapping("/authors")
    public List<AuthorResponse> getAuthors() {
        return userService.findAuthors();
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }

    @GetMapping("/me")
    public UserResponse findMine(Authentication authentication) {
        return userService.findMine(userService.requireUserId(authentication));
    }

    @PutMapping("/me")
    public UserResponse updateMine(Authentication authentication, @Valid @RequestBody UserRequest request) {
        return userService.updateMine(userService.requireUserId(authentication), request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateById(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return userService.updateById(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable UUID id) {
        userService.deleteById(id);
    }
}
