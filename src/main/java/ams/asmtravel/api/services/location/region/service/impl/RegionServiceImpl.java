package ams.asmtravel.api.services.location.region.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.location.region.dto.response.GeoBounds;
import ams.asmtravel.api.services.taxonomy.category.domain.CategoryDeletingEvent;
import ams.asmtravel.api.services.taxonomy.category.service.CategoryService;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionDeletingEvent;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.location.region.dto.request.RegionRequest;
import ams.asmtravel.api.services.location.region.dto.response.RegionResponse;
import ams.asmtravel.api.services.location.region.exception.RegionNotFoundException;
import ams.asmtravel.api.services.location.region.mapper.RegionMapper;
import ams.asmtravel.api.services.location.region.repository.RegionRepository;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
import ams.asmtravel.api.services.taxonomy.tag.domain.TagDeletingEvent;
import ams.asmtravel.api.services.taxonomy.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    /** The code column's width. */
    private static final int CODE_LENGTH = 20;

    /** In the curators' order; a region not numbered yet after them, A to Z. */
    private static final Comparator<Region> IN_ORDER = Comparator
            .comparing(Region::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Region::getName, String.CASE_INSENSITIVE_ORDER);

    private final RegionRepository regionRepository;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final InterestService interestService;
    private final RegionMapper mapper;
    private final ApplicationEventPublisher events;
    /* Destinations depend on this service for their regions, so they are reached lazily, for the box around them. */
    private final ObjectProvider<DestinationService> destinationService;

    @Override
    @Transactional(readOnly = true)
    public List<RegionResponse> getAll(RegionKind kind) {
        List<Region> regions = kind == null
                ? regionRepository.findAllByOrderByNameAsc()
                : regionRepository.findAllByKindOrderByNameAsc(kind);
        List<DestinationSummary> places = places();
        return regions.stream().sorted(IN_ORDER).map(region -> mapper.toRegionResponse(region, boundsOf(region, places))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RegionResponse findById(UUID id) {
        return respond(find(id));
    }

    @Override
    @Transactional
    public RegionResponse create(RegionRequest request) {
        String code = isBlank(request.code()) ? codeFrom(request.name()) : request.code().trim().toUpperCase(Locale.ROOT);
        if (regionRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another region already uses the code " + code);
        }
        List<Region> others = inKind(request.kind() != null ? request.kind() : RegionKind.ZONE, null);
        Region region = new Region();
        region.setCode(code);
        apply(region, request, null);
        region.setIsActive(request.isActive() == null || request.isActive());
        placeIn(region, others, request.sortOrder());
        return respond(regionRepository.save(region));
    }

    @Override
    @Transactional
    public RegionResponse update(UUID id, RegionRequest request) {
        Region region = find(id);
        RegionKind was = region.getKind();
        RegionKind kind = request.kind() != null ? request.kind() : was;
        List<Region> others = inKind(kind, id);
        Integer place = request.sortOrder() != null || kind != was ? request.sortOrder() : region.getSortOrder();
        String code = isBlank(request.code()) ? region.getCode() : request.code().trim().toUpperCase(Locale.ROOT);
        if (regionRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another region already uses the code " + code);
        }
        region.setCode(code);
        apply(region, request, id);
        if (request.isActive() != null) {
            region.setIsActive(request.isActive());
        }
        placeIn(region, others, place);
        Region saved = regionRepository.saveAndFlush(region);
        if (kind != was) {
            renumber(was, id);
        }
        return respond(saved);
    }

    /** Refused while provinces are in it; preferences on it are cleared; child zones become top level. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Region region = find(id);
        events.publishEvent(new RegionDeletingEvent(id));
        regionRepository.detachChildren(id);
        regionRepository.delete(region);
        renumber(region.getKind(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && regionRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Region getEntity(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A region is required");
        }
        return regionRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such region"));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Region> getTourismEntities(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> wanted = new HashSet<>(ids);
        Set<Region> found = new LinkedHashSet<>(regionRepository.findAllById(wanted));
        if (found.size() != wanted.size()) {
            found.stream().map(Region::getId).forEach(wanted::remove);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such region: " + wanted.iterator().next());
        }
        for (Region region : found) {
            if (region.getKind() != RegionKind.TOURISM) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        region.getName() + " is a zone: a place is in it through its province, so choose tourism regions only");
            }
        }
        return found;
    }

    /** A zone may sit in another, but not in itself or in one of its own children. */
    private void apply(Region region, RegionRequest request, UUID id) {
        UUID parentId = request.parentId();
        for (UUID ancestor = parentId; ancestor != null; ancestor = find(ancestor).getParentId()) {
            if (ancestor.equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A region cannot sit inside itself");
            }
            if (!regionRepository.existsById(ancestor)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such parent region");
            }
        }
        region.setName(request.name().trim());
        region.setParentId(parentId);
        region.setDescription(request.description());
        if (request.coverKey() != null) {
            region.setCoverKey(request.coverKey().isBlank() ? null : request.coverKey().trim());
        }
        if (request.latitude() != null || request.longitude() != null) {
            if (request.latitude() == null || request.longitude() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A centre needs both a latitude and a longitude");
            }
            region.setLatitude(request.latitude());
            region.setLongitude(request.longitude());
        }
        if (request.kind() != null) {
            region.setKind(request.kind());
        }
        /* Null keeps the links, so a client that does not send them cannot empty them. */
        if (request.categoryIds() != null || region.getId() == null) {
            List<UUID> ids = request.categoryIds() == null ? List.of() : request.categoryIds();
            region.setCategories(inOrder(ids, categoryService.getEntities(ids), category -> category.getId()));
        }
        if (request.tagIds() != null || region.getId() == null) {
            List<UUID> ids = request.tagIds() == null ? List.of() : request.tagIds();
            region.setTags(inOrder(ids, tagService.getEntities(ids), tag -> tag.getId()));
        }
        if (request.interestIds() != null || region.getId() == null) {
            region.setInterests(new HashSet<>(interestService.getEntities(
                    request.interestIds() == null ? List.of() : request.interestIds())));
        }
    }

    private Region find(UUID id) {
        return regionRepository.findWithLinksById(id).orElseThrow(() -> new RegionNotFoundException());
    }

    private RegionResponse respond(Region region) {
        return mapper.toRegionResponse(region, boundsOf(region, places()));
    }

    private List<DestinationSummary> places() {
        return destinationService.getObject().findPublishedSummaries();
    }

    /** The box around the published places in a zone (through their province) or shelved in a tourism region. */
    private static GeoBounds boundsOf(Region region, List<DestinationSummary> places) {
        return GeoBounds.around(places.stream()
                .filter(place -> place.inRegion(region.getId()))
                .map(place -> new GeoBounds.Point(place.latitude(), place.longitude()))
                .toList());
    }

    /** The rows found, in the order the ids were sent, each once. A new list, so the old link rows are replaced whole. */
    private static <T> List<T> inOrder(List<UUID> ids, Collection<T> found, Function<T, UUID> idOf) {
        Map<UUID, T> byId = found.stream().collect(Collectors.toMap(idOf, Function.identity()));
        return new ArrayList<>(ids.stream().distinct().map(byId::get).filter(java.util.Objects::nonNull).toList());
    }

    /** The regions of a kind in their order, leaving one out; read before anything is changed. */
    private List<Region> inKind(RegionKind kind, UUID without) {
        return new ArrayList<>(regionRepository.findAllByKind(kind).stream()
                .filter(other -> !other.getId().equals(without))
                .sorted(IN_ORDER)
                .toList());
    }

    /** Puts a region among the others at its place — the end when none is given — and numbers them all 0, 1, 2… */
    private static void placeIn(Region region, List<Region> others, Integer place) {
        int at = place == null ? others.size() : Math.max(0, Math.min(place, others.size()));
        others.add(at, region);
        for (int index = 0; index < others.size(); index++) {
            others.get(index).setSortOrder(index);
        }
    }

    /** Numbers the regions of a kind again, leaving one out: the one deleted, or the one that left. */
    private void renumber(RegionKind kind, UUID without) {
        List<Region> left = regionRepository.findAllByKind(kind).stream()
                .filter(other -> !other.getId().equals(without))
                .sorted(IN_ORDER)
                .toList();
        for (int index = 0; index < left.size(); index++) {
            left.get(index).setSortOrder(index);
        }
    }

    /** A deleted category leaves every region's shelves; the list is replaced whole, so the rest keep their order. */
    @EventListener
    public void onCategoryDeleting(CategoryDeletingEvent event) {
        for (Region region : regionRepository.findDistinctByCategories_Id(event.categoryId())) {
            region.setCategories(new ArrayList<>(region.getCategories().stream()
                    .filter(category -> category != null && !category.getId().equals(event.categoryId())).toList()));
        }
        regionRepository.flush();
    }

    /** A deleted tag leaves every region's filters, the same way. */
    @EventListener
    public void onTagDeleting(TagDeletingEvent event) {
        for (Region region : regionRepository.findDistinctByTags_Id(event.tagId())) {
            region.setTags(new ArrayList<>(region.getTags().stream()
                    .filter(tag -> tag != null && !tag.getId().equals(event.tagId())).toList()));
        }
        regionRepository.flush();
    }

    /** A code from the name when none is sent: "Wellness & Spa" is WELLNESS-SPA, cut to fit 20 characters, numbered (-2) if taken. */
    private String codeFrom(String name) {
        String plain = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String words = plain.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("^-+|-+$", "");
        String base = cut(words.isEmpty() ? "REGION" : words, CODE_LENGTH);
        String code = base;
        for (int n = 2; regionRepository.existsByCodeIgnoreCase(code); n++) {
            String suffix = "-" + n;
            code = cut(base, CODE_LENGTH - suffix.length()) + suffix;
        }
        return code;
    }

    /** At most {@code length} characters, ending at a whole word where one fits. */
    private static String cut(String code, int length) {
        if (code.length() <= length) {
            return code;
        }
        String head = code.substring(0, length);
        int word = head.lastIndexOf('-');
        return (word > 0 ? head.substring(0, word) : head).replaceAll("-+$", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return regionRepository.count();
    }
}
