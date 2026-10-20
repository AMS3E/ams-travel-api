package ams.asmtravel.api.services.taxonomy.interest.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.interest.domain.InterestDeletingEvent;
import ams.asmtravel.api.services.taxonomy.interest.dto.request.InterestRequest;
import ams.asmtravel.api.services.taxonomy.interest.dto.response.InterestResponse;
import ams.asmtravel.api.services.taxonomy.interest.exception.InterestNotFoundException;
import ams.asmtravel.api.services.taxonomy.interest.mapper.InterestMapper;
import ams.asmtravel.api.services.taxonomy.interest.repository.InterestRepository;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    /** In the curators' order within a group; one not numbered yet after them, A to Z. */
    private static final Comparator<Interest> IN_ORDER = Comparator
            .comparing(Interest::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Interest::getName, String.CASE_INSENSITIVE_ORDER);

    private final InterestRepository interestRepository;
    private final InterestMapper mapper;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<InterestResponse> getAll() {
        return interestRepository.findAllByOrderByNameAsc().stream().sorted(IN_ORDER).map(mapper::toInterestResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InterestResponse findById(UUID id) {
        return mapper.toInterestResponse(find(id));
    }

    @Override
    @Transactional
    public InterestResponse create(InterestRequest request) {
        List<Interest> others = inGroup(groupOf(request), null);
        Interest interest = new Interest();
        interest.setSlug(Slugs.forCreate(request.slug(), request.name(), interestRepository::existsBySlug));
        apply(interest, request);
        interest.setIsActive(request.isActive() == null || request.isActive());
        placeIn(interest, others, request.sortOrder());
        interest.setCreatedBy(author());
        return mapper.toInterestResponse(interestRepository.save(interest));
    }

    @Override
    @Transactional
    public InterestResponse update(UUID id, InterestRequest request) {
        Interest interest = find(id);
        String was = interest.getGroupName();
        boolean moved = !Objects.equals(was, groupOf(request));
        List<Interest> others = inGroup(groupOf(request), id);
        Integer place = request.sortOrder() != null || moved ? request.sortOrder() : interest.getSortOrder();
        interest.setSlug(Slugs.forUpdate(request.slug(), interest.getSlug(), slug -> interestRepository.existsBySlugAndIdNot(slug, id)));
        apply(interest, request);
        if (request.isActive() != null) {
            interest.setIsActive(request.isActive());
        }
        placeIn(interest, others, place);
        Interest saved = interestRepository.saveAndFlush(interest);
        if (moved) {
            renumber(was, id);
        }
        return mapper.toInterestResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Interest interest = find(id);
        events.publishEvent(new InterestDeletingEvent(id));
        interestRepository.delete(interest);
        renumber(interest.getGroupName(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Interest> getEntities(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> wanted = new HashSet<>(ids);
        Set<Interest> found = new LinkedHashSet<>(interestRepository.findAllById(wanted));
        if (found.size() != wanted.size()) {
            found.stream().map(Interest::getId).forEach(wanted::remove);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such interest: " + wanted.iterator().next());
        }
        return found;
    }

    private void apply(Interest interest, InterestRequest request) {
        interest.setName(request.name().trim());
        interest.setDescription(request.description());
        if (request.nameKm() != null) {
            interest.setNameKm(request.nameKm().isBlank() ? null : request.nameKm().trim());
        }
        if (request.descriptionKm() != null) {
            interest.setDescriptionKm(request.descriptionKm().isBlank() ? null : request.descriptionKm().trim());
        }
        interest.setGroupName(groupOf(request));
        if (request.iconKey() != null) {
            interest.setIconKey(request.iconKey().isBlank() ? null : request.iconKey().trim());
        }
        if (request.coverKey() != null) {
            interest.setCoverKey(request.coverKey().isBlank() ? null : request.coverKey().trim());
        }
    }

    /** The group it is listed under, as saved: trimmed, and null for none. */
    private static String groupOf(InterestRequest request) {
        return request.groupName() == null || request.groupName().isBlank() ? null : request.groupName().trim();
    }

    /** A group's interests in their order, leaving one out; read before anything is changed. */
    private List<Interest> inGroup(String group, UUID without) {
        return new ArrayList<>(interestRepository.findAll().stream()
                .filter(other -> Objects.equals(other.getGroupName(), group) && !other.getId().equals(without))
                .sorted(IN_ORDER)
                .toList());
    }

    /** Places an interest in its group (at the end when no place is given) and numbers them 0, 1, 2… */
    private static void placeIn(Interest interest, List<Interest> others, Integer place) {
        int at = place == null ? others.size() : Math.max(0, Math.min(place, others.size()));
        others.add(at, interest);
        for (int index = 0; index < others.size(); index++) {
            others.get(index).setSortOrder(index);
        }
    }

    /** Numbers a group again, leaving one out: the one deleted, or the one that moved to another group. */
    private void renumber(String group, UUID without) {
        List<Interest> left = interestRepository.findAll().stream()
                .filter(other -> Objects.equals(other.getGroupName(), group) && !other.getId().equals(without))
                .sorted(IN_ORDER)
                .toList();
        for (int index = 0; index < left.size(); index++) {
            left.get(index).setSortOrder(index);
        }
    }

    private Interest find(UUID id) {
        return interestRepository.findById(id).orElseThrow(() -> new InterestNotFoundException());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return interestRepository.count();
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
