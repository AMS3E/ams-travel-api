package ams.asmtravel.api.services.search.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * One province's row on the Provinces page: its first cards for the filters asked, and how many match in all.
 * Provinces with nothing matching are not listed. Asked in Khmer, {@code untranslated} holds "provinceName" when none is written.
 */
public record ProvinceGroupResponse(UUID provinceId, String provinceName, long totalElements, List<SearchHitResponse> content,
                                    List<String> untranslated) {
}
