package ams.asmtravel.api.services.user.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.dto.request.IdentityRequest;
import ams.asmtravel.api.services.user.user.dto.response.IdentityResponse;
import ams.asmtravel.api.services.user.user.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The Keycloak accounts people sign in with. Read-only: they are recorded at sign-in. */
@Tag(name = "People · Identities", description = "How an account signs in.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identities")
public class IdentityController {

    private final IdentityService identityService;

    @GetMapping
    public List<IdentityResponse> getAll(IdentityRequest request) {
        return identityService.getAll(request);
    }
}
