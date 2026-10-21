package ams.asmtravel.api.services.search.controller;

import ams.asmtravel.api.services.search.domain.SearchType;
import ams.asmtravel.api.services.search.dto.request.SearchRequest;
import ams.asmtravel.api.services.search.dto.response.ProvinceGroupResponse;
import ams.asmtravel.api.services.search.dto.response.SearchHitResponse;
import ams.asmtravel.api.services.search.dto.response.SuggestionResponse;
import ams.asmtravel.api.services.search.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
     * {@code ?type=DESTINATION|STAY|FOOD|ACTIVITY|CORRIDOR} (destinations when left out), {@code ?q=} words, then
     * {@code regionIds}, {@code provinceIds}, {@code categoryIds}, {@code tagIds}, {@code interestIds} (ids, comma-separated;
     * {@code regionId} and {@code provinceId} still take one), {@code travelerRecommended=true} for the seal holders,
     * {@code near=} a destination's id for what lies around it, nearest first;
     * {@code ?page=&size=} with 20 a page and at most 100. Name order. {@code ?lang=km} (here and on every search URL)
     * gives the cards' words in Khmer where written, naming what is still English in {@code untranslated}.
     */
    @GetMapping
    public Page<SearchHitResponse> search(@Valid @ModelAttribute SearchRequest request,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return searchService.search(request, pageable);
    }

    /**
     * The Provinces page in one answer: the same filters, grouped by province, each with its first
     * {@code perProvince} cards (8 unless asked, at most 20) and how many match in all. Provinces with nothing left out.
     */
    @GetMapping("/by-province")
    public List<ProvinceGroupResponse> byProvince(@Valid @ModelAttribute SearchRequest request,
                                                  @RequestParam(defaultValue = "8") @Min(1) @Max(20) int perProvince) {
        return searchService.byProvince(request, perProvince);
    }

    /** Names containing the text, those starting with it first: places, provinces, tourism regions and corridors. */
    @GetMapping("/suggest")
    public List<SuggestionResponse> suggest(@RequestParam @NotBlank @Size(max = 100) String q,
                                            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit,
                                            @RequestParam(required = false) @Pattern(regexp = "^(en|km)?$") String lang) {
        return searchService.suggest(q, limit, "km".equals(lang));
    }

    /** The cards for known ids, in the order asked: what Saves, Popular and Nearby draw their rows from. */
    @GetMapping("/cards")
    public List<SearchHitResponse> cards(@RequestParam(defaultValue = "DESTINATION") SearchType type,
                                         @RequestParam @Size(min = 1, max = 100) List<UUID> ids,
                                         @RequestParam(required = false) @Pattern(regexp = "^(en|km)?$") String lang) {
        return searchService.cards(type, ids, "km".equals(lang));
    }
}
