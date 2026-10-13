package ams.asmtravel.api.services.admin.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.admin.dto.request.OverviewRequest;
import ams.asmtravel.api.services.admin.dto.response.DestinationStatsResponse;
import ams.asmtravel.api.services.admin.dto.response.OverviewResponse;
import ams.asmtravel.api.services.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    /**
     * The Destination Stats page: every destination's ratings, reviews and
     * browses over a period, with its views, Verified standing and status as
     * of today. {@code ?from=&to=} are inclusive dates in Cambodia; neither for
     * all time.
     */
    @GetMapping("/destination-stats")
    public DestinationStatsResponse getDestinationStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return adminService.destinationStats(from, to);
    }
}
