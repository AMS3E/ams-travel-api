package ams.asmtravel.api.services.taxonomy.tag.service;

import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import ams.asmtravel.api.services.taxonomy.tag.dto.request.TagRequest;
import ams.asmtravel.api.services.taxonomy.tag.dto.response.TagResponse;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TagService {

    List<TagResponse> getAll();

    TagResponse findById(UUID id);

    TagResponse create(TagRequest request);

    TagResponse update(UUID id, TagRequest request);

    /** Destinations and food lose the tag. */
    void delete(UUID id);

    /** @throws org.springframework.web.server.ResponseStatusException 400 when any id is unknown */
    Set<Tag> getEntities(Collection<UUID> ids);

    /** How many there are: a tile on the admin overview. */
    long count();
}
