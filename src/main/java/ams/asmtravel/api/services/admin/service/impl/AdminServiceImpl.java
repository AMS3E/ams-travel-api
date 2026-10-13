package ams.asmtravel.api.services.admin.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.admin.dto.request.OverviewRequest;
import ams.asmtravel.api.services.admin.dto.response.DestinationStatsResponse;
import ams.asmtravel.api.services.admin.dto.response.OverviewResponse;
import ams.asmtravel.api.services.admin.mapper.AdminMapper;
import ams.asmtravel.api.services.admin.repository.OverviewRepository;
import ams.asmtravel.api.services.admin.service.AdminService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.service.ReviewService.ReviewTotals;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Override
    public DestinationStatsResponse destinationStats(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The period ends before it starts");
        }
        ZoneId zone = overviewRepository.zone();
        boolean allTime = from == null && to == null;
        Instant start = from == null ? Instant.EPOCH : from.atStartOfDay(zone).toInstant();
        Instant end = to == null ? Instant.now().plus(1, ChronoUnit.DAYS) : to.plusDays(1).atStartOfDay(zone).toInstant();
        List<DestinationSummary> places = overviewRepository.destinations();
        List<UUID> ids = places.stream().map(DestinationSummary::id).toList();
        Map<UUID, Long> browses = overviewRepository.browses(start, end);
        Map<UUID, ReviewSummaryResponse> reviews = overviewRepository.reviewSummaries(ids, allTime ? null : start, allTime ? null : end);
        Map<UUID, VerificationResponse> standing = overviewRepository.verificationStatuses(ids);
        return new DestinationStatsResponse(from, to, places.stream()
                .map(place -> mapper.toPlace(place, browses.get(place.id()), reviews.get(place.id()), standing.get(place.id())))
                .toList());
    }
}
