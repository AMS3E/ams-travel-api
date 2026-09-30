package ams.asmtravel.api.services.taxonomy.category.service;

import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.category.dto.request.CategoryRequest;
import ams.asmtravel.api.services.taxonomy.category.dto.response.CategoryResponse;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> getAll();

    CategoryResponse findById(UUID id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(UUID id, CategoryRequest request);

    /** Destinations and food lose the category. */
    void delete(UUID id);

    /**
     * The rows, for a destination or food to be linked to.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 when any id is unknown
     */
    Set<Category> getEntities(Collection<UUID> ids);

    /** How many there are: a tile on the admin overview. */
    long count();
}
