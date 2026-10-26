package ams.asmtravel.api.services.taxonomy.interestgroup.service.impl;

import ams.asmtravel.api.services.taxonomy.interestgroup.domain.InterestGroup;
import ams.asmtravel.api.services.taxonomy.interestgroup.dto.request.InterestGroupRequest;
import ams.asmtravel.api.services.taxonomy.interestgroup.dto.response.InterestGroupResponse;
import ams.asmtravel.api.services.taxonomy.interestgroup.exception.InterestGroupNotFoundException;
import ams.asmtravel.api.services.taxonomy.interestgroup.mapper.InterestGroupMapper;
import ams.asmtravel.api.services.taxonomy.interestgroup.repository.InterestGroupRepository;
import ams.asmtravel.api.services.taxonomy.interestgroup.service.InterestGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterestGroupServiceImpl implements InterestGroupService {

    private final InterestGroupRepository interestGroupRepository;
    private final InterestGroupMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<InterestGroupResponse> getAll() {
        return interestGroupRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(mapper::toInterestGroupResponse)
                .toList();
    }

    @Override
    @Transactional
    public InterestGroupResponse update(UUID id, InterestGroupRequest request) {
        InterestGroup group = interestGroupRepository.findById(id).orElseThrow(InterestGroupNotFoundException::new);
        if (request.coverKey() != null) {
            group.setCoverKey(request.coverKey().isBlank() ? null : request.coverKey().trim());
        }
        return mapper.toInterestGroupResponse(group);
    }
}
