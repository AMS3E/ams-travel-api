package ams.asmtravel.api.services.search.dto.response;

import ams.asmtravel.api.services.search.domain.SuggestionKind;

import java.util.UUID;

import java.util.List;

/** A name the search box offers while the visitor types; {@code untranslated} holds "name" when asked in Khmer and none is written. */
public record SuggestionResponse(SuggestionKind kind, UUID id, String name, String slug, List<String> untranslated) {
}
