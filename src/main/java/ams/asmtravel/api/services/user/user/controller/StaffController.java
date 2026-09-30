package ams.asmtravel.api.services.user.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.dto.request.StaffRequest;
import ams.asmtravel.api.services.user.user.dto.response.StaffResponse;
import ams.asmtravel.api.services.user.user.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

/** Staff accounts and roles in Keycloak; ids are Keycloak ids. */
@Tag(name = "People · Staff", description = "Back-office accounts and the roles they hold.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffService keycloakUserService;

    /** Back-office accounts, or anyone in the realm when a search is given. */
    @GetMapping
    public List<StaffResponse> getAll(@RequestParam(required = false) String search) {
        return search == null || search.isBlank()
                ? keycloakUserService.findBackofficeUsers()
                : keycloakUserService.search(search.trim());
    }

    @GetMapping("/{id}")
    public StaffResponse getById(@PathVariable String id) {
        return keycloakUserService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public StaffResponse create(@Valid @RequestBody StaffRequest request) {
        return keycloakUserService.create(request);
    }

    /** Sets the complete set of back-office roles, adding and revoking to match. */
    @PutMapping("/{id}/roles")
    public StaffResponse setRoles(@PathVariable String id, @Valid @RequestBody StaffRequest request) {
        return keycloakUserService.setRoles(id, request.roles());
    }
}
