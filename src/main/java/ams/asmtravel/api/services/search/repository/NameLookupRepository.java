package ams.asmtravel.api.services.search.repository;

import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.search.domain.KhmerNames;
import ams.asmtravel.api.services.taxonomy.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The names a card prints beside ids: not a table of this service's own, but the taxonomy's and the provinces', read whole. */
@Repository
@RequiredArgsConstructor
public class NameLookupRepository {

    private final ProvinceService provinceService;
    private final CategoryService categoryService;

    public Map<UUID, String> provinceNames() {
        Map<UUID, String> names = new HashMap<>();
        provinceService.findSummaries().forEach(province -> names.put(province.id(), province.name()));
        return names;
    }

    public Map<UUID, String> categoryNames() {
        Map<UUID, String> names = new HashMap<>();
        categoryService.getAll().forEach(category -> names.put(category.id(), category.name()));
        return names;
    }

    /** Province, category and region names in Khmer, only those written, with the English categories beside them. */
    public KhmerNames khmerNames() {
        Map<UUID, String> provinces = new HashMap<>();
        provinceService.findSummaries().stream().filter(province -> hasText(province.nameKm()))
                .forEach(province -> provinces.put(province.id(), province.nameKm()));
        Map<UUID, String> categories = new HashMap<>();
        Map<UUID, String> categoriesKm = new HashMap<>();
        categoryService.getAll().forEach(category -> {
            categories.put(category.id(), category.name());
            if (hasText(category.nameKm())) {
                categoriesKm.put(category.id(), category.nameKm());
            }
        });
        return new KhmerNames(provinces, categories, categoriesKm);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
