package ams.asmtravel.api.services.history.regioncoverage.service.impl;

import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.history.period.dto.response.HistoricalPeriodResponse;
import ams.asmtravel.api.services.history.period.service.HistoricalPeriodService;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.history.regioncoverage.domain.RegionCoverage;
import ams.asmtravel.api.services.history.regioncoverage.dto.request.RegionCoverageRequest;
import ams.asmtravel.api.services.history.regioncoverage.dto.response.RegionCoverageResponse;
import ams.asmtravel.api.services.history.regioncoverage.exception.RegionCoverageNotFoundException;
import ams.asmtravel.api.services.history.regioncoverage.mapper.RegionCoverageMapper;
import ams.asmtravel.api.services.history.regioncoverage.repository.RegionCoverageRepository;
import ams.asmtravel.api.services.history.regioncoverage.service.RegionCoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegionCoverageServiceImpl implements RegionCoverageService {

    private final RegionCoverageRepository coverageRepository;
    private final HistoricalPeriodService periodService;
    private final RegionService regionService;
    private final DestinationService destinationService;
    private final RegionCoverageMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<RegionCoverageResponse> getAll(UUID regionId) {
        List<RegionCoverage> rows = regionId == null
                ? coverageRepository.findAllByOrderByRegionNameAscSortOrderAsc()
                : coverageRepository.findByRegionIdOrderBySortOrderAsc(regionId);
        /* One call for every era on the list, rather than one per row. */
        Map<UUID, HistoricalPeriodResponse> periods = periodService.findAllById(
                rows.stream().map(RegionCoverage::getHistoricalPeriodId).filter(Objects::nonNull).toList());
        return rows.stream().map(row -> respond(row, periods)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RegionCoverageResponse findById(UUID id) {
        return respond(find(id));
    }

    @Override
    @Transactional
    public RegionCoverageResponse create(RegionCoverageRequest request) {
        RegionCoverage coverage = new RegionCoverage();
        apply(coverage, request);
        RegionCoverage saved = coverageRepository.save(coverage);
        placeIn(saved, request.sortOrder());
        return respond(saved);
    }

    @Override
    @Transactional
    public RegionCoverageResponse update(UUID id, RegionCoverageRequest request) {
        RegionCoverage coverage = find(id);
        UUID leftRegion = coverage.getRegion().getId();
        Integer place = request.sortOrder() != null ? request.sortOrder() : coverage.getSortOrder();
        apply(coverage, request);
        RegionCoverage saved = coverageRepository.saveAndFlush(coverage);
        placeIn(saved, place);
        if (!leftRegion.equals(saved.getRegion().getId())) {
            renumber(new ArrayList<>(coverageRepository.findByRegionIdOrderBySortOrderAsc(leftRegion)));
        }
        return respond(saved);
    }

    /** The ones after it move up a place. */
    @Override
    @Transactional
    public void delete(UUID id) {
        RegionCoverage coverage = find(id);
        UUID region = coverage.getRegion().getId();
        coverageRepository.delete(coverage);
        coverageRepository.flush();
        renumber(new ArrayList<>(coverageRepository.findByRegionIdOrderBySortOrderAsc(region)));
    }

    /** Moves an entry to its place in the region's list (0 first, none = last) and renumbers the rest. */
    private void placeIn(RegionCoverage entry, Integer place) {
        List<RegionCoverage> others = new ArrayList<>(coverageRepository.findByRegionIdOrderBySortOrderAsc(entry.getRegion().getId()));
        others.removeIf(other -> other.getId().equals(entry.getId()));
        int at = place == null ? others.size() : Math.max(0, Math.min(place, others.size()));
        others.add(at, entry);
        renumber(others);
    }

    private void renumber(List<RegionCoverage> inOrder) {
        for (int index = 0; index < inOrder.size(); index++) {
            inOrder.get(index).setSortOrder(index);
        }
        coverageRepository.saveAllAndFlush(inOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return coverageRepository.count();
    }

    private void apply(RegionCoverage coverage, RegionCoverageRequest request) {
        coverage.setRegion(regionService.getEntity(request.regionId()));
        coverage.setTitle(request.title().trim());
        coverage.setTitleKm(blankToNull(request.titleKm()));
        coverage.setDateRange(blankToNull(request.dateRange()));
        coverage.setDescription(request.description());
        if (request.historicalPeriodId() != null && !periodService.exists(request.historicalPeriodId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such historical period");
        }
        coverage.setHistoricalPeriodId(request.historicalPeriodId());
        coverage.setDestination(request.destinationId() == null
                ? null : destinationService.getEntity(request.destinationId()));
    }

    /** The era's name beside its id, so a reader needs no second call. */
    private RegionCoverageResponse respond(RegionCoverage coverage) {
        UUID period = coverage.getHistoricalPeriodId();
        return respond(coverage, period == null ? Map.of() : periodService.findAllById(List.of(period)));
    }

    private RegionCoverageResponse respond(RegionCoverage coverage, Map<UUID, HistoricalPeriodResponse> periods) {
        HistoricalPeriodResponse period = coverage.getHistoricalPeriodId() == null
                ? null : periods.get(coverage.getHistoricalPeriodId());
        return mapper.toRegionCoverageResponse(coverage, period == null ? null : period.name());
    }

    private RegionCoverage find(UUID id) {
        return coverageRepository.findWithLinksById(id)
                .orElseThrow(() -> new RegionCoverageNotFoundException());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
