package ams.asmtravel.api.services.admin.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedImages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the Cambodia catalogue into an empty database on startup.
 * Skipped when any service already has rows; {@code app.seed.catalog=false} turns it off.
 */
@Component
@ConditionalOnProperty(name = "app.seed.catalog", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CatalogSeedRunner implements ApplicationRunner {

    /** Injected in {@link CatalogSeedContributor#getOrder()} order by Spring. */
    private final List<CatalogSeedContributor> contributors;
    private final SeedImages seedImages;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        CatalogSeedContext context = new CatalogSeedContext(seedImages.publish());

        List<String> populated = contributors.stream()
                .filter(CatalogSeedContributor::alreadySeeded)
                .map(contributor -> contributor.getClass().getSimpleName())
                .toList();
        if (!populated.isEmpty()) {
            log.info("Catalogue already present ({} reports rows), skipping seed", populated.getFirst());
            return;
        }

        for (CatalogSeedContributor contributor : contributors) {
            contributor.seed(context);
        }
        log.info("Seeded the catalogue from {} contributions", contributors.size());
    }
}
