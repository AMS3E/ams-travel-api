package ams.asmtravel.api.services.admin.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.admin.dto.request.OverviewRequest;
import ams.asmtravel.api.services.admin.dto.response.DestinationStatsResponse;
import ams.asmtravel.api.services.admin.dto.response.OverviewResponse;
import ams.asmtravel.api.services.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The dashboards' overview tiles. */
@Tag(name = "System · Overview", description = "The counts and figures the dashboard opens on.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/overview")
    public OverviewResponse getOverview(Authentication authentication, @Valid @ModelAttribute OverviewRequest request) {
        return adminService.overview(request, authentication);
    }

    /** The Destination Stats page: every destination's ratings, browses and Verified standing. */
    @GetMapping("/destination-stats")
    public DestinationStatsResponse getDestinationStats() {
        return adminService.destinationStats();
    }
}
