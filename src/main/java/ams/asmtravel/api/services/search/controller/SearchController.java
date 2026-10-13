package ams.asmtravel.api.services.search.controller;

import ams.asmtravel.api.services.search.domain.SearchType;
import ams.asmtravel.api.services.search.dto.request.SearchRequest;
import ams.asmtravel.api.services.search.dto.response.SearchHitResponse;
import ams.asmtravel.api.services.search.dto.response.SuggestionResponse;
import ams.asmtravel.api.services.search.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** What the app's search box, filter chips, lists and cards read. Public: published places only. */
@Tag(name = "Discover · Search", description = "The app's search, its filters, and the cards its lists are made of.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    /**
     * {@code ?type=DESTINATION|STAY|FOOD|CORRIDOR} (destinations when left out), {@code ?q=} words, then
     * {@code regionId}, {@code provinceId}, {@code categoryIds}, {@code tagIds}, {@code interestIds} (ids, comma-separated);
     * {@code ?page=&size=} with 20 a page and at most 100. Name order.
     */
    @GetMapping
    public Page<SearchHitResponse> search(@Valid @ModelAttribute SearchRequest request,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return searchService.search(request, pageable);
    }

    /** Names containing the text, those starting with it first: places, provinces, tourism regions and corridors. */
    @GetMapping("/suggest")
    public List<SuggestionResponse> suggest(@RequestParam @NotBlank @Size(max = 100) String q,
                                            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit) {
        return searchService.suggest(q, limit);
    }

    /** The cards for known ids, in the order asked: what Saves, Popular and Nearby draw their rows from. */
    @GetMapping("/cards")
    public List<SearchHitResponse> cards(@RequestParam(defaultValue = "DESTINATION") SearchType type,
                                         @RequestParam @Size(min = 1, max = 100) List<UUID> ids) {
        return searchService.cards(type, ids);
    }
}
