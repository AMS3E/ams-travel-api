package ams.asmtravel.api.services.activity.popular.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.activity.popular.config.PopularProperties;
import ams.asmtravel.api.services.activity.popular.domain.BrowseEvent;
import ams.asmtravel.api.services.activity.popular.domain.BrowseKinds;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTotal;
import ams.asmtravel.api.services.activity.popular.domain.PopularPlace;
import ams.asmtravel.api.services.activity.popular.domain.PopularReason;
import ams.asmtravel.api.services.activity.popular.dto.request.BrowseEventRequest;
import ams.asmtravel.api.services.activity.popular.dto.response.PopularResponse;
import ams.asmtravel.api.services.activity.popular.mapper.PopularMapper;
import ams.asmtravel.api.services.activity.popular.repository.BrowseEventRepository;
import ams.asmtravel.api.services.activity.popular.service.PopularService;
import ams.asmtravel.api.services.location.region.exception.RegionNotFoundException;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Browses and Popular, from published places only, the most browsed first. */
@Service
@RequiredArgsConstructor
public class PopularServiceImpl implements PopularService {

    private final BrowseEventRepository eventRepository;
    private final DestinationService destinationService;
    private final RegionService regionService;
    private final PopularMapper mapper;
    private final PopularProperties properties;

    @Override
    @Transactional
    public void record(BrowseEventRequest request, UUID userId) {
        boolean firstToday = request.destinationId() != null && !seenToday(request.destinationId(), userId,
                request.sessionId());
        BrowseEvent event = mapper.toEvent(request, userId);
        if (event.getMetadata() == null) {
            event.setMetadata(new LinkedHashMap<>());
        }
        eventRepository.save(event);
        if (firstToday && (userId != null || request.sessionId() != null) && destinationService.exists(request.destinationId())) {
            destinationService.recordView(request.destinationId());
        }
    }

    @Override
    @Transactional
    public boolean recordBrowse(UUID userId, UUID destinationId) {
        boolean firstToday = !seenToday(destinationId, userId, null);
        BrowseEvent event = new BrowseEvent();
        event.setUserId(userId);
        event.setDestinationId(destinationId);
        event.setEventType(BrowseKinds.EXPLORATION);
        eventRepository.save(event);
        if (firstToday) {
            destinationService.recordView(destinationId);
        }
        return firstToday;
    }

    @Override
    @Transactional(readOnly = true)
    public PopularResponse popular(UUID regionId, Integer limit) {
        if (regionId != null && !regionService.exists(regionId)) {
            throw new RegionNotFoundException();
        }
        int size = limit == null ? properties.size() : limit;

        // Published places, only in the named zone or region if one is given.
        Map<UUID, DestinationSummary> catalogue = new LinkedHashMap<>();
        destinationService.findPublishedSummaries().stream()
                .filter(place -> regionId == null || place.inRegion(regionId))
                .forEach(place -> catalogue.put(place.id(), place));

        // Whatever visitors browse most, published places only.
        List<PopularPlace> places = new ArrayList<>();
        for (BrowseTotal total : recentTotals()) {
            if (places.size() >= size) {
                break;
            }
            if (total.browses() > 0 && catalogue.containsKey(total.destinationId())) {
                places.add(new PopularPlace(total.destinationId(), total.browses(), PopularReason.MOST_BROWSED));
            }
        }
        return mapper.toResponse(properties.days(), regionId, places);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrowseTotal> recentTotals() {
        LocalDate first = LocalDate.now(properties.zone()).minusDays(properties.days() - 1L);
        return eventRepository.totalsSince(first.atStartOfDay(properties.zone()).toInstant());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> browsedDestinationIds(UUID userId) {
        return userId == null ? Set.of() : Set.copyOf(eventRepository.findBrowsedDestinationIds(userId));
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        eventRepository.forgetUser(event.userId());
    }

    /** Distinct (place, Cambodian day) pairs: opening Angkor Wat twice on Monday is one browse. */
    @Override
    @Transactional(readOnly = true)
    public long countDailyBrowses(UUID userId, Instant from, Instant to) {
        Set<String> daily = new HashSet<>();
        for (BrowseEvent event : eventRepository.findByUserBetween(userId, from, to)) {
            daily.add(event.getDestinationId() + "@" + event.getCreatedAt().atZone(properties.zone()).toLocalDate());
        }
        return daily.size();
    }

    /** Whether this visitor — or, anonymous, this session — opened the place earlier today in Cambodia. */
    private boolean seenToday(UUID destinationId, UUID userId, String sessionId) {
        ZonedDateTime start = LocalDate.now(properties.zone()).atStartOfDay(properties.zone());
        Instant from = start.toInstant();
        Instant to = start.plusDays(1).toInstant();
        if (userId != null) {
            return eventRepository.existsByDestinationIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    destinationId, userId, from, to);
        }
        return sessionId != null
                && eventRepository.existsByDestinationIdAndSessionIdAndUserIdIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        destinationId, sessionId, from, to);
    }
}
