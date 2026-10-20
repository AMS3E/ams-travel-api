package ams.asmtravel.api.services.admin.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedImages;
import ams.asmtravel.api.services.admin.domain.SeedRun;
import ams.asmtravel.api.services.admin.repository.SeedRunRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seeds the Cambodia catalogue into an empty database on startup. Skipped when any service already
 * has rows, unless {@code app.seed.reset-token} names a token no earlier run recorded: then every
 * service wipes what it seeded, in reverse order, and the catalogue is seeded afresh. Staff edits to
 * seeded rows go with the reset, so the token is for a staging server, set once per fresh dataset.
 * {@code app.seed.catalog=false} turns the whole thing off.
 */
@Component
@ConditionalOnProperty(name = "app.seed.catalog", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CatalogSeedRunner implements ApplicationRunner {

    /** Injected in {@link CatalogSeedContributor#getOrder()} order by Spring. */
    private final List<CatalogSeedContributor> contributors;
    private final SeedImages seedImages;
    private final SeedRunRepository seedRuns;
    private final String resetToken;

    public CatalogSeedRunner(List<CatalogSeedContributor> contributors, SeedImages seedImages, SeedRunRepository seedRuns,
                             @Value("${app.seed.reset-token:}") String resetToken) {
        this.contributors = contributors;
        this.seedImages = seedImages;
        this.seedRuns = seedRuns;
        this.resetToken = resetToken == null ? "" : resetToken.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> populated = contributors.stream()
                .filter(CatalogSeedContributor::alreadySeeded)
                .map(contributor -> contributor.getClass().getSimpleName())
                .toList();
        boolean reset = !resetToken.isEmpty() && !seedRuns.existsById(resetToken);
        if (!populated.isEmpty() && !reset) {
            log.info("Catalogue already present ({} reports rows), skipping seed", populated.getFirst());
            return;
        }
        CatalogSeedContext context = new CatalogSeedContext(seedImages.publish());
        if (!populated.isEmpty()) {
            log.warn("Seed reset \"{}\": wiping the seeded catalogue ({} services report rows)", resetToken, populated.size());
            List<CatalogSeedContributor> reversed = new ArrayList<>(contributors);
            Collections.reverse(reversed);
            reversed.forEach(CatalogSeedContributor::wipe);
        }
        for (CatalogSeedContributor contributor : contributors) {
            contributor.seed(context);
        }
        if (!resetToken.isEmpty()) {
            seedRuns.save(new SeedRun(resetToken));
        }
        log.info("Seeded the catalogue from {} contributions{}", contributors.size(), reset ? " after a reset" : "");
    }
}
