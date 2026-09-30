package ams.asmtravel.api.services.taxonomy.tag.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.taxonomy.tag.dto.request.TagRequest;
import ams.asmtravel.api.services.taxonomy.tag.dto.response.TagResponse;
import ams.asmtravel.api.services.taxonomy.tag.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Free labels on destinations and food. */
@Tag(name = "Discover · Tags", description = "The search filters a place carries.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    @GetMapping
    public List<TagResponse> getAll() {
        return tagService.getAll();
    }

    @GetMapping("/{id}")
    public TagResponse getById(@PathVariable UUID id) {
        return tagService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public TagResponse create(@Valid @RequestBody TagRequest request) {
        return tagService.create(request);
    }

    @PutMapping("/{id}")
    public TagResponse update(@PathVariable UUID id, @Valid @RequestBody TagRequest request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tagService.delete(id);
    }
}
