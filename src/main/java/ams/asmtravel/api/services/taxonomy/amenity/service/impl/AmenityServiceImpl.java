package ams.asmtravel.api.services.taxonomy.amenity.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.taxonomy.amenity.domain.Amenity;
import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityDeletingEvent;
import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityLevel;
import ams.asmtravel.api.services.taxonomy.amenity.dto.request.AmenityRequest;
import ams.asmtravel.api.services.taxonomy.amenity.dto.response.AmenityResponse;
import ams.asmtravel.api.services.taxonomy.amenity.exception.AmenityNotFoundException;
import ams.asmtravel.api.services.taxonomy.amenity.mapper.AmenityMapper;
import ams.asmtravel.api.services.taxonomy.amenity.repository.AmenityRepository;
import ams.asmtravel.api.services.taxonomy.amenity.service.AmenityService;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;
    private final AmenityMapper mapper;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> getAll(AmenityLevel level) {
        return amenityRepository.findAllByOrderByGroupNameAscSortOrderAscNameAsc().stream()
                .filter(amenity -> level == null || amenity.getLevel().offeredAt(level))
                .map(mapper::toAmenityResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AmenityResponse findById(UUID id) {
        return mapper.toAmenityResponse(find(id));
    }

    @Override
    @Transactional
    public AmenityResponse create(AmenityRequest request) {
        Amenity amenity = new Amenity();
        amenity.setSlug(Slugs.forCreate(request.slug(), request.name(), amenityRepository::existsBySlug));
        apply(amenity, request);
        /* A new one goes last in its group unless told where. */
        amenity.setSortOrder(request.sortOrder() != null
                ? request.sortOrder()
                : amenityRepository.lastSortOrder(amenity.getGroupName()) + 1);
        amenity.setIsActive(request.isActive() == null || request.isActive());
        amenity.setCreatedBy(author());
        return mapper.toAmenityResponse(amenityRepository.save(amenity));
    }

    @Override
    @Transactional
    public AmenityResponse update(UUID id, AmenityRequest request) {
        Amenity amenity = find(id);
        amenity.setSlug(Slugs.forUpdate(request.slug(), amenity.getSlug(),
                slug -> amenityRepository.existsBySlugAndIdNot(slug, id)));
        apply(amenity, request);
        if (request.sortOrder() != null) {
            amenity.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            amenity.setIsActive(request.isActive());
        }
        return mapper.toAmenityResponse(amenityRepository.saveAndFlush(amenity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Amenity amenity = find(id);
        /* A stay or a room that still lists the slug answers with 409: retire it instead. */
        events.publishEvent(new AmenityDeletingEvent(id, amenity.getSlug()));
        amenityRepository.delete(amenity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> unknown(List<String> slugs, AmenityLevel level) {
        if (slugs == null || slugs.isEmpty()) {
            return List.of();
        }
        Set<String> known = amenityRepository.findBySlugIn(slugs).stream()
                .filter(amenity -> amenity.getLevel().offeredAt(level))
                .map(Amenity::getSlug)
                .collect(Collectors.toSet());
        return slugs.stream().filter(slug -> !known.contains(slug)).distinct().sorted().toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return amenityRepository.count();
    }

    private void apply(Amenity amenity, AmenityRequest request) {
        amenity.setName(request.name().trim());
        amenity.setGroupName(request.groupName().trim());
        amenity.setIconKey(request.iconKey() == null || request.iconKey().isBlank() ? null : request.iconKey().trim());
        amenity.setLevel(request.level());
    }

    private Amenity find(UUID id) {
        return amenityRepository.findById(id).orElseThrow(AmenityNotFoundException::new);
    }

    /** The caller's user id, or null for a token without identity (a service call). */
    private UUID author() {
        try {
            return userService.findUserId(Authorities.current()).orElse(null);
        } catch (ResponseStatusException noIdentity) {
            return null;
        }
    }
}
