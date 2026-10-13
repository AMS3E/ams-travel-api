package ams.asmtravel.api.services.search.dto.response;

import ams.asmtravel.api.services.search.domain.SuggestionKind;

import java.util.UUID;

/** A name the search box offers while the visitor types. */
public record SuggestionResponse(SuggestionKind kind, UUID id, String name, String slug) {
}
