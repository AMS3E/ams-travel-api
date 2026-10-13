package ams.asmtravel.api.services.search.service;

import ams.asmtravel.api.services.search.domain.SearchType;
import ams.asmtravel.api.services.search.dto.request.SearchRequest;
import ams.asmtravel.api.services.search.dto.response.SearchHitResponse;
import ams.asmtravel.api.services.search.dto.response.SuggestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** What the app's search box, filter chips, lists and cards read: published places only, in name order. */
public interface SearchService {

    /** A page of cards matching the request; the page size is capped at 100. */
    Page<SearchHitResponse> search(SearchRequest request, Pageable pageable);

    /** Up to {@code limit} names containing the text, those starting with it first. */
    List<SuggestionResponse> suggest(String text, int limit);

    /** The cards of known ids, in the order asked; unknown and unpublished ids are left out. */
    List<SearchHitResponse> cards(SearchType type, List<UUID> ids);
}
