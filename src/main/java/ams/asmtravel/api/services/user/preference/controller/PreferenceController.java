package ams.asmtravel.api.services.user.preference.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.preference.dto.request.PreferenceRequest;
import ams.asmtravel.api.services.user.preference.dto.response.PreferenceResponse;
import ams.asmtravel.api.services.user.preference.service.PreferenceService;
import ams.asmtravel.api.services.user.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in person's own preferences: language, currency, home region, notifications, theme. */
@Tag(name = "People · Preferences", description = "What a customer told us they like.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;
    private final UserService userService;

    @GetMapping("/me")
    public PreferenceResponse findMine(Authentication authentication) {
        return preferenceService.find(userService.requireUserId(authentication));
    }

    @PutMapping("/me")
    public PreferenceResponse updateMine(Authentication authentication, @Valid @RequestBody PreferenceRequest request) {
        return preferenceService.update(userService.requireUserId(authentication), request);
    }
}
