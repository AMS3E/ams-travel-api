package ams.asmtravel.api.services.user.preference.service.impl;

import ams.asmtravel.api.services.user.preference.domain.UserPreferences;
import ams.asmtravel.api.services.user.preference.dto.request.PreferenceRequest;
import ams.asmtravel.api.services.user.preference.dto.response.PreferenceResponse;
import ams.asmtravel.api.services.user.preference.mapper.PreferenceMapper;
import ams.asmtravel.api.services.user.preference.repository.PreferenceRepository;
import ams.asmtravel.api.services.user.preference.service.PreferenceService;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.location.region.domain.RegionDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreferenceServiceImpl implements PreferenceService {

    private final PreferenceRepository preferenceRepository;
    private final PreferenceMapper preferenceMapper;
    private final UserService userService;

    /** Clears a deleted region from everyone's preferences; runs in the delete's transaction. */
    @EventListener
    public void onRegionDeleting(RegionDeletingEvent event) {
        preferenceRepository.clearPreferredRegion(event.regionId());
    }

    @Override
    @Transactional(readOnly = true)
    public PreferenceResponse find(UUID userId) {
        return preferenceMapper.toResponse(preferenceRepository.findById(userId).orElseGet(UserPreferences::new));
    }

    /** A missing region fails the database key to taxonomy.regions: a 400, not a conflict. */
    @Override
    @Transactional
    public PreferenceResponse update(UUID userId, PreferenceRequest request) {
        UserPreferences preferences = preferenceRepository.findById(userId)
                .orElseGet(() -> new UserPreferences(userService.getEntity(userId)));
        preferenceMapper.update(request, preferences);
        try {
            return preferenceMapper.toResponse(preferenceRepository.saveAndFlush(preferences));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such region");
        }
    }
}
