package ams.asmtravel.api.services.taxonomy.interestgroup.dto.request;

import ams.asmtravel.api.services.media.dto.request.MediaKey;

/**
 * @param coverKey the uploaded picture's key; null keeps the current one, empty removes it
 */
public record InterestGroupRequest(
        @MediaKey(allowEmpty = true, message = "Upload the picture first, then send its key") String coverKey
) {
}
