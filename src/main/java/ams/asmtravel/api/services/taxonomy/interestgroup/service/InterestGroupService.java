package ams.asmtravel.api.services.taxonomy.interestgroup.service;

import ams.asmtravel.api.services.taxonomy.interestgroup.dto.request.InterestGroupRequest;
import ams.asmtravel.api.services.taxonomy.interestgroup.dto.response.InterestGroupResponse;

import java.util.List;
import java.util.UUID;

public interface InterestGroupService {

    /** In the row's order. */
    List<InterestGroupResponse> getAll();

    /** Changes its picture; the group itself is fixed. */
    InterestGroupResponse update(UUID id, InterestGroupRequest request);
}
