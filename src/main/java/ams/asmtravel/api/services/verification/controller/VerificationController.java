package ams.asmtravel.api.services.verification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.verification.dto.request.VerificationRequest;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import ams.asmtravel.api.services.verification.service.VerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Verification decisions and their evidence. Only {@code /status} is public. */
@Tag(name = "Publishing · Verification", description = "Verification decisions and the evidence behind them.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/destination-verifications")
public class VerificationController {

    private final VerificationService verificationService;
    private final UserService userService;

    /** One place's decisions, newest first. */
    @GetMapping
    public List<VerificationResponse> getHistory(Authentication authentication, @RequestParam UUID destinationId) {
        return verificationService.findHistory(destinationId, authentication);
    }

    /** Places the caller may verify; scoped by the service, not by caller filters. */
    @GetMapping("/queue")
    public List<VerificationResponse> getQueue(Authentication authentication) {
        return verificationService.findQueue(authentication);
    }

    /** Verified status for the website; without ids, all verified published places. */
    @GetMapping("/status")
    public List<VerificationResponse> getStatuses(@RequestParam(required = false) @Size(max = 200) List<UUID> destinationIds) {
        return verificationService.findStatuses(destinationIds == null ? List.of() : destinationIds);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public VerificationResponse record(Authentication authentication, @Valid @RequestBody VerificationRequest request) {
        return verificationService.record(request, userService.requireUserId(authentication), authentication);
    }
}
