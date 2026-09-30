package ams.asmtravel.api.services.admin.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.admin.dto.request.OverviewRequest;
import ams.asmtravel.api.services.admin.dto.response.OverviewResponse;
import ams.asmtravel.api.services.admin.mapper.AdminMapper;
import ams.asmtravel.api.services.admin.repository.OverviewRepository;
import ams.asmtravel.api.services.admin.service.AdminService;
import ams.asmtravel.api.services.review.review.service.ReviewService.ReviewTotals;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static ams.asmtravel.api.services.authorization.domain.Permissions.FULL_CONTROL;

/** The moderators see reviews and the season; full control sees the table counts as well. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final OverviewRepository overviewRepository;
    private final AdminMapper mapper;

    @Override
    public OverviewResponse overview(OverviewRequest request, Authentication authentication) {
        SeasonResponse season = overviewRepository.currentSeason().orElse(null);
        ReviewTotals totals = overviewRepository.reviewTotals(season);
        Map<String, Long> counts = null;
        if (Authorities.has(authentication, FULL_CONTROL)) {
            counts = overviewRepository.counts();
            counts.put("reviews", totals.total());
        }
        return new OverviewResponse(counts, mapper.toReviews(totals, season != null), season,
                overviewRepository.recentReviews(request.recentOrDefault()));
    }
}
