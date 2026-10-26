package ams.asmtravel.api.services.taxonomy.interestgroup.controller;

import ams.asmtravel.api.services.taxonomy.interestgroup.dto.request.InterestGroupRequest;
import ams.asmtravel.api.services.taxonomy.interestgroup.dto.response.InterestGroupResponse;
import ams.asmtravel.api.services.taxonomy.interestgroup.service.InterestGroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The groups interests are listed under, and the picture each one shows. */
@Tag(name = "Discover · Interest groups", description = "The six groups of the Interests tab and their pictures.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interest-groups")
public class InterestGroupController {

    private final InterestGroupService interestGroupService;

    @GetMapping
    public List<InterestGroupResponse> getAll() {
        return interestGroupService.getAll();
    }

    @PutMapping("/{id}")
    public InterestGroupResponse update(@PathVariable UUID id, @Valid @RequestBody InterestGroupRequest request) {
        return interestGroupService.update(id, request);
    }
}
