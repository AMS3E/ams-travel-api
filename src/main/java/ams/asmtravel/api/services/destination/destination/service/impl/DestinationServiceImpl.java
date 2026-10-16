package ams.asmtravel.api.services.destination.destination.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSearch;
import ams.asmtravel.api.services.destination.destination.specification.DestinationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ams.asmtravel.api.services.history.architecture.domain.ArchitectureStyleDeletingEvent;
import ams.asmtravel.api.services.history.architecture.service.ArchitectureStyleService;
import ams.asmtravel.api.services.taxonomy.category.domain.CategoryDeletingEvent;
import ams.asmtravel.api.services.taxonomy.category.service.CategoryService;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.domain.DestinationRelocatedEvent;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.DestinationThemes;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.domain.NearbyDestination;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.destination.destination.dto.request.DestinationRequest;
import ams.asmtravel.api.services.destination.destination.dto.response.DestinationResponse;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.mapper.DestinationMapper;
import ams.asmtravel.api.services.destination.destination.repository.DestinationRepository;
import ams.asmtravel.api.services.destination.destination.security.DestinationPermissionEvaluator;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.taxonomy.interest.domain.InterestDeletingEvent;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
import ams.asmtravel.api.services.location.province.domain.ProvinceDeletingEvent;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.domain.RegionDeletingEvent;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriodDeletingEvent;
import ams.asmtravel.api.services.history.period.service.HistoricalPeriodService;
import ams.asmtravel.api.services.history.religion.domain.ReligionDeletingEvent;
import ams.asmtravel.api.services.history.religion.service.ReligionService;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.taxonomy.tag.domain.TagDeletingEvent;
import ams.asmtravel.api.services.taxonomy.tag.service.TagService;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DestinationServiceImpl implements DestinationService {

    private final DestinationRepository destinationRepository;
    private final ProvinceService provinceService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final InterestService interestService;
    private final RegionService regionService;
    private final ArchitectureStyleService architectureStyleService;
    private final ReligionService religionService;
    private final HistoricalPeriodService historicalPeriodService;
    private final UserService userService;
    private final DestinationPermissionEvaluator permission;
    private final DestinationMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<DestinationResponse> getPublished(String slug) {
        if (slug != null && !slug.isBlank()) {
            return destinationRepository.findBySlugAndStatus(slug.trim(), ListingStatus.PUBLISHED)
                    .map(mapper::toDestinationResponse)
                    .stream().toList();
        }
        return destinationRepository.findAllByStatusOrderByNameAsc(ListingStatus.PUBLISHED).stream()
                .map(mapper::toDestinationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DestinationResponse> searchPublished(DestinationSearch search, Pageable pageable) {
        return destinationRepository.findAll(DestinationSpecification.published(search), pageable).map(mapper::toDestinationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationResponse> findPublished(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return destinationRepository.findAllByIdIn(ids).stream()
                .filter(destination -> destination.getStatus() == ListingStatus.PUBLISHED)
                .map(mapper::toDestinationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationResponse> suggestPublished(String text, int limit) {
        if (text == null || text.isBlank() || limit < 1) {
            return List.of();
        }
        return destinationRepository.findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(ListingStatus.PUBLISHED, text.trim(), PageRequest.of(0, limit)).stream()
                .map(mapper::toDestinationResponse)
                .toList();
    }

    /** An unpublished place outside the caller's desk is 404, not 403. */
    @Override
    @Transactional(readOnly = true)
    public DestinationResponse findById(UUID id) {
        Destination destination = find(id);
        if (!destination.isPublished() && !permission.canRead(destination, authentication())) {
            throw new DestinationNotFoundException();
        }
        return mapper.toDestinationResponse(destination);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationResponse> findNearby(double latitude, double longitude, double radiusKm, int limit) {
        List<UUID> ids = destinationRepository.findNearbyIds(latitude, longitude, radiusKm * 1_000, limit);
        Map<UUID, Destination> byId = destinationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(mapper::toDestinationResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyDestination> findNearbyWithDistance(double latitude, double longitude, double radiusKm, int limit) {
        List<DestinationRepository.NearbyDistance> rows = destinationRepository.findNearbyDistances(latitude, longitude, radiusKm * 1_000, limit);
        Map<UUID, Destination> byId = destinationRepository.findAllById(rows.stream().map(DestinationRepository.NearbyDistance::getId).toList())
                .stream().collect(Collectors.toMap(Destination::getId, Function.identity()));
        return rows.stream()
                .filter(row -> byId.containsKey(row.getId()))
                .map(row -> new NearbyDestination(mapper.toDestinationResponse(byId.get(row.getId())), row.getMetres()))
                .toList();
    }

    @Override
    @Transactional
    public DestinationResponse create(DestinationRequest request, UUID authorId) {
        Destination destination = new Destination();
        destination.setSlug(slugFor(request.slug(), request.name(), null));
        apply(destination, request);
        destination.setStatus(ListingStatus.DRAFT);
        destination.setCreatedBy(authorId);
        destination = destinationRepository.save(destination);
        log.info("Destination {} ({}) drafted by {}", destination.getName(), destination.getId(), authorId);
        return mapper.toDestinationResponse(destination);
    }

    @Override
    @Transactional
    public DestinationResponse update(UUID id, DestinationRequest request) {
        Destination destination = find(id);
        if (request.slug() != null && !request.slug().isBlank() && !request.slug().equals(destination.getSlug())) {
            destination.setSlug(slugFor(request.slug(), request.name(), id));
        }
        BigDecimal latitude = destination.getLatitude();
        BigDecimal longitude = destination.getLongitude();
        apply(destination, request);
        DestinationResponse saved = mapper.toDestinationResponse(destinationRepository.saveAndFlush(destination));
        if (!sameNumber(latitude, destination.getLatitude()) || !sameNumber(longitude, destination.getLongitude())) {
            events.publishEvent(new DestinationRelocatedEvent(id));
        }
        return saved;
    }

    /** Other services clean up on the deleting event first; database cascades remove the rest. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Destination destination = find(id);
        events.publishEvent(new DestinationDeletingEvent(id));
        destinationRepository.delete(destination);
        destinationRepository.flush();
        log.info("Destination {} ({}) deleted", destination.getName(), id);
    }

    @Override
    @Transactional
    public DestinationResponse publish(UUID id) {
        Destination destination = find(id);
        destination.setStatus(ListingStatus.PUBLISHED);
        destination.setPublishedAt(Instant.now());
        return mapper.toDestinationResponse(destinationRepository.saveAndFlush(destination));
    }

    @Override
    @Transactional
    public DestinationResponse unpublish(UUID id) {
        Destination destination = find(id);
        destination.setStatus(ListingStatus.DRAFT);
        return mapper.toDestinationResponse(destinationRepository.saveAndFlush(destination));
    }

    /** Editors see all; a BDO sees partners' rows plus their own; everyone else their own. */
    @Override
    @Transactional
    public List<DestinationResponse> getWorkspace() {
        Authentication authentication = authentication();
        List<Destination> visible;
        if (permission.canReadAll(authentication)) {
            visible = destinationRepository.findAllByOrderByNameAsc();
        } else {
            Set<UUID> authors = new HashSet<>();
            if (permission.canReadPartnerListings(authentication)) {
                authors.addAll(userService.findUserIdsHoldingAny(Set.of(RoleCode.PARTNER)));
            }
            authors.add(userService.requireUserId(authentication));
            visible = destinationRepository.findAllByCreatedByInOrderByNameAsc(authors).stream()
                    .filter(destination -> permission.canRead(destination, authentication))
                    .toList();
        }
        return visible.stream().map(mapper::toDestinationResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DestinationSummary> findSummary(UUID id) {
        return id == null ? Optional.empty() : destinationRepository.findWithLinksById(id).map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, DestinationSummary> findSummaries(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, DestinationSummary> summaries = new LinkedHashMap<>();
        destinationRepository.findAllByIdIn(Set.copyOf(ids))
                .forEach(destination -> summaries.put(destination.getId(), mapper.toSummary(destination)));
        return summaries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationSummary> findPublishedSummaries() {
        return destinationRepository.findAllByStatusOrderByNameAsc(ListingStatus.PUBLISHED).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationSummary> findAllSummaries() {
        return destinationRepository.findAllByOrderByNameAsc().stream().map(mapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Destination> getEntities(Collection<UUID> ids) {
        return ids == null || ids.isEmpty() ? List.of() : destinationRepository.findAllById(Set.copyOf(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public Destination getEntity(UUID id) {
        return destinationRepository.findById(id).orElseThrow(() -> new DestinationNotFoundException());
    }

    @Override
    @Transactional
    public boolean canEdit(UUID id, Authentication authentication) {
        return permission.canEdit(id, authentication);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRead(UUID id, Authentication authentication) {
        return destinationRepository.findById(id)
                .map(destination -> destination.isPublished() || permission.canRead(destination, authentication))
                .orElse(false);
    }

    @Override
    @Transactional
    public void setCover(UUID id, String coverKey) {
        Destination destination = getEntity(id);
        destination.setCoverKey(coverKey);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && destinationRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublished(UUID id) {
        return id != null && destinationRepository.findById(id).map(Destination::isPublished).orElse(false);
    }

    @Override
    @Transactional
    public void recordView(UUID id) {
        destinationRepository.incrementViewCount(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return destinationRepository.count();
    }

    @EventListener
    public void onCategoryDeleting(CategoryDeletingEvent event) {
        destinationRepository.unlinkCategory(event.categoryId());
    }

    @EventListener
    public void onTagDeleting(TagDeletingEvent event) {
        destinationRepository.unlinkTag(event.tagId());
    }

    @EventListener
    public void onInterestDeleting(InterestDeletingEvent event) {
        destinationRepository.unlinkInterest(event.interestId());
    }

    @EventListener
    public void onRegionDeleting(RegionDeletingEvent event) {
        destinationRepository.unlinkRegion(event.regionId());
    }

    @EventListener
    public void onArchitectureStyleDeleting(ArchitectureStyleDeletingEvent event) {
        destinationRepository.unlinkArchitectureStyle(event.styleId());
    }

    @EventListener
    public void onReligionDeleting(ReligionDeletingEvent event) {
        destinationRepository.unlinkReligion(event.religionId());
    }

    @EventListener
    public void onHistoricalPeriodDeleting(HistoricalPeriodDeletingEvent event) {
        destinationRepository.unlinkHistoricalPeriod(event.periodId());
    }

    /** A province can be deleted only once its destinations have moved. */
    @EventListener
    public void onProvinceDeleting(ProvinceDeletingEvent event) {
        if (destinationRepository.existsByProvinceId(event.provinceId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destinations are still in this province; move them first");
        }
    }

    private void apply(Destination destination, DestinationRequest request) {
        destination.setName(request.name().trim());
        destination.setProvince(provinceService.getEntity(request.provinceId()));
        destination.setShortDescription(blankToNull(request.shortDescription()));
        destination.setDescription(blankToNull(request.description()));
        destination.setSlogan(blankToNull(request.slogan()));
        destination.setSloganBy(blankToNull(request.sloganBy()));
        destination.setLatitude(request.latitude());
        destination.setLongitude(request.longitude());
        destination.setAddress(blankToNull(request.address()));
        destination.setCoverKey(blankToNull(request.coverKey()));
        if (request.categoryIds() != null || destination.getId() == null) {
            destination.setCategories(new HashSet<>(categoryService.getEntities(nullToEmpty(request.categoryIds()))));
        }
        if (request.tagIds() != null || destination.getId() == null) {
            destination.setTags(new HashSet<>(tagService.getEntities(nullToEmpty(request.tagIds()))));
        }
        if (request.interestIds() != null || destination.getId() == null) {
            destination.setInterests(new HashSet<>(interestService.getEntities(nullToEmpty(request.interestIds()))));
        }
        if (request.tourismRegionIds() != null || destination.getId() == null) {
            destination.setTourismRegions(new HashSet<>(regionService.getTourismEntities(nullToEmpty(request.tourismRegionIds()))));
        }
        if (request.architectureStyleIds() != null || destination.getId() == null) {
            destination.setArchitectureStyles(new HashSet<>(
                    architectureStyleService.getEntities(nullToEmpty(request.architectureStyleIds()))));
        }
        if (request.religionIds() != null || destination.getId() == null) {
            destination.setReligions(new HashSet<>(religionService.getEntities(nullToEmpty(request.religionIds()))));
        }
        /* Ancient Capitals filters: Historical Period and Century. */
        if (request.historicalPeriodIds() != null || destination.getId() == null) {
            destination.setHistoricalPeriods(new HashSet<>(
                    historicalPeriodService.getEntities(nullToEmpty(request.historicalPeriodIds()))));
        }
        if (request.centuries() != null || destination.getId() == null) {
            destination.setCenturies(new ArrayList<>(request.centuries() == null ? List.of()
                    : request.centuries().stream().distinct().sorted().toList()));
        }
        /* Theme and its specialties; values from an old theme are kept. */
        String theme = blankToNull(request.theme());
        /* The nine first regions, or any tourism region made since: a new one simply has no specialties. */
        if (theme != null && !DestinationThemes.isTheme(theme) && !regionService.isTourismCode(theme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such theme: " + theme);
        }
        if (theme != null) {
            destination.setTheme(theme);
        }
        if (request.specialties() != null || destination.getId() == null) {
            List<String> specialties = request.specialties() == null ? List.of()
                    : request.specialties().stream().map(String::trim).filter(one -> !one.isEmpty()).distinct().toList();
            List<String> unknown = DestinationThemes.unknown(specialties);
            if (!unknown.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such specialty: " + String.join(", ", unknown));
            }
            destination.setSpecialties(new ArrayList<>(specialties));
        }
    }

    private String slugFor(String wanted, String name, UUID id) {
        if (wanted != null && !wanted.isBlank()) {
            boolean taken = id == null ? destinationRepository.existsBySlug(wanted)
                    : destinationRepository.existsBySlugAndIdNot(wanted, id);
            if (taken) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another destination already uses the slug " + wanted);
            }
            return wanted;
        }
        return Slugs.unique(Slugs.of(name), destinationRepository::existsBySlug);
    }

    private Destination find(UUID id) {
        return destinationRepository.findWithLinksById(id)
                .orElseThrow(() -> new DestinationNotFoundException());
    }

    private static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** 13.41 and 13.4100000 are the same latitude. */
    private static boolean sameNumber(BigDecimal before, BigDecimal after) {
        return before == null ? after == null : after != null && before.compareTo(after) == 0;
    }

    private static <T> Set<T> nullToEmpty(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
