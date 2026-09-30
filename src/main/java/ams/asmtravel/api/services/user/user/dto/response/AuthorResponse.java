package ams.asmtravel.api.services.user.user.dto.response;

import java.util.UUID;

/**
 * An account's id and name only, to show who wrote a row; full accounts need {@code USER_MANAGE}.
 *
 * @param id       the account's id, as a row's {@code createdBy} names it
 * @param fullName never empty: the e-mail's name part when the profile has none
 */
public record AuthorResponse(UUID id, String fullName) {
}
