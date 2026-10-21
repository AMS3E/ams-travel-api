package ams.asmtravel.api.services.taxonomy.tag.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import ams.asmtravel.api.services.taxonomy.tag.domain.TagDeletingEvent;
import ams.asmtravel.api.services.taxonomy.tag.dto.request.TagRequest;
import ams.asmtravel.api.services.taxonomy.tag.dto.response.TagResponse;
import ams.asmtravel.api.services.taxonomy.tag.exception.TagNotFoundException;
import ams.asmtravel.api.services.taxonomy.tag.mapper.TagMapper;
import ams.asmtravel.api.services.taxonomy.tag.repository.TagRepository;
import ams.asmtravel.api.services.taxonomy.tag.service.TagService;
import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper mapper;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAll() {
        return tagRepository.findAllByOrderByNameAsc().stream().map(mapper::toTagResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse findById(UUID id) {
        return mapper.toTagResponse(find(id));
    }

    @Override
    @Transactional
    public TagResponse create(TagRequest request) {
        Tag tag = new Tag();
        tag.setSlug(Slugs.forCreate(request.slug(), request.name(), tagRepository::existsBySlug));
        apply(tag, request);
        tag.setIsActive(request.isActive() == null || request.isActive());
        tag.setCreatedBy(author());
        return mapper.toTagResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public TagResponse update(UUID id, TagRequest request) {
        Tag tag = find(id);
        tag.setSlug(Slugs.forUpdate(request.slug(), tag.getSlug(), slug -> tagRepository.existsBySlugAndIdNot(slug, id)));
        apply(tag, request);
        if (request.isActive() != null) {
            tag.setIsActive(request.isActive());
        }
        return mapper.toTagResponse(tagRepository.saveAndFlush(tag));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Tag tag = find(id);
        events.publishEvent(new TagDeletingEvent(id));
        tagRepository.delete(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Tag> getEntities(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> wanted = new HashSet<>(ids);
        Set<Tag> found = new LinkedHashSet<>(tagRepository.findAllById(wanted));
        if (found.size() != wanted.size()) {
            found.stream().map(Tag::getId).forEach(wanted::remove);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such tag: " + wanted.iterator().next());
        }
        return found;
    }

    private void apply(Tag tag, TagRequest request) {
        tag.setName(request.name().trim());
        if (request.nameKm() != null) {
            tag.setNameKm(request.nameKm().isBlank() ? null : request.nameKm().trim());
        }
        tag.setDescription(request.description());
    }

    private Tag find(UUID id) {
        return tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return tagRepository.count();
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
