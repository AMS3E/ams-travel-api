package ams.asmtravel.api.services.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** @param recent how many of the latest reviews to show; null for 5 */
public record OverviewRequest(@Min(1) @Max(20) Integer recent) {

    public int recentOrDefault() {
        return recent == null ? 5 : recent;
    }
}
