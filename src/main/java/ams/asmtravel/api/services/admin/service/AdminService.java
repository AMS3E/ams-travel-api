package ams.asmtravel.api.services.admin.service;

import ams.asmtravel.api.services.admin.dto.request.OverviewRequest;
import ams.asmtravel.api.services.admin.dto.response.DestinationStatsResponse;
import ams.asmtravel.api.services.admin.dto.response.OverviewResponse;
import org.springframework.security.core.Authentication;

/** The dashboards' summary tiles; the admin service owns no rows. */
public interface AdminService {

    /** Reviews, the current badge period and the latest reviews; table counts too, for full control. */
    OverviewResponse overview(OverviewRequest request, Authentication authentication);

    /** Every destination's ratings, reviews, browses, views and Verified standing. */
    DestinationStatsResponse destinationStats();
}
