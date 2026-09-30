package ams.asmtravel.api.services.user.preference.service;

import ams.asmtravel.api.services.user.preference.dto.request.PreferenceRequest;
import ams.asmtravel.api.services.user.preference.dto.response.PreferenceResponse;

import java.util.UUID;

public interface PreferenceService {

    /** Their preferences, or the defaults when they have saved none. */
    PreferenceResponse find(UUID userId);

    /**
     * @throws org.springframework.web.server.ResponseStatusException 400
     *         when the preferred region does not exist
     */
    PreferenceResponse update(UUID userId, PreferenceRequest request);
}
