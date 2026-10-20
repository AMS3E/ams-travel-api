package ams.asmtravel.api.services.taxonomy.category.service.impl;

import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.category.domain.CategoryDeletingEvent;
import ams.asmtravel.api.services.taxonomy.category.dto.request.CategoryRequest;
import ams.asmtravel.api.services.taxonomy.category.dto.response.CategoryResponse;
import ams.asmtravel.api.services.taxonomy.category.exception.CategoryNotFoundException;
import ams.asmtravel.api.services.taxonomy.category.mapper.CategoryMapper;
import ams.asmtravel.api.services.taxonomy.category.repository.CategoryRepository;
import ams.asmtravel.api.services.taxonomy.category.service.CategoryService;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
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
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream().map(mapper::toCategoryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return mapper.toCategoryResponse(find(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        category.setSlug(Slugs.forCreate(request.slug(), request.name(), categoryRepository::existsBySlug));
        apply(category, request);
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setIsActive(request.isActive() == null || request.isActive());
        category.setCreatedBy(author());
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = find(id);
        category.setSlug(Slugs.forUpdate(request.slug(), category.getSlug(), slug -> categoryRepository.existsBySlugAndIdNot(slug, id)));
        apply(category, request);
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        return mapper.toCategoryResponse(categoryRepository.saveAndFlush(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Category category = find(id);
        events.publishEvent(new CategoryDeletingEvent(id));
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Category> getEntities(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> wanted = new HashSet<>(ids);
        Set<Category> found = new LinkedHashSet<>(categoryRepository.findAllById(wanted));
        if (found.size() != wanted.size()) {
            found.stream().map(Category::getId).forEach(wanted::remove);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such category: " + wanted.iterator().next());
        }
        return found;
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        if (request.nameKm() != null) {
            category.setNameKm(request.nameKm().isBlank() ? null : request.nameKm().trim());
        }
        category.setDescription(request.description());
        category.setIconKey(request.iconKey() == null || request.iconKey().isBlank() ? null : request.iconKey().trim());
    }

    private Category find(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return categoryRepository.count();
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
