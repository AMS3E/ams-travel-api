package ams.asmtravel.api.services.media.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedImages;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import java.util.UUID;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.media.client.MinioStorageClient;
import ams.asmtravel.api.services.media.domain.Media;
import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.repository.MediaRepository;
import ams.asmtravel.api.services.media.service.impl.MediaServiceImpl;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** MinIO at startup: the client, one folder per category, the seed photos and the seeded galleries. */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final String SEED_IMAGES = "classpath:ams/asmtravel/api/services/media/config/images/*";

    @Bean
    MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.minio().endpoint())
                .credentials(properties.minio().accessKey(), properties.minio().secretKey())
                .build();
    }

    /** If MinIO is down it only logs; the API still starts. */
    @Bean
    @Order(0)
    @ConditionalOnProperty(name = "media.prepare-folders", havingValue = "true", matchIfMissing = true)
    ApplicationRunner mediaFolders(MinioStorageClient storage) {
        return args -> {
            try {
                log.info("Media folders ready ({} created)", storage.ensureFolders());
            } catch (ResponseStatusException exception) {
                log.warn("Media folders not prepared — storage is unreachable: {}", exception.getReason());
            }
        };
    }

    /** Seed photos under {@code general/} with readable names; empty when MinIO is unreachable. */
    @Bean
    SeedImages seedImages(MediaServiceImpl mediaService) {
        return () -> {
            Map<String, String> keys = new LinkedHashMap<>();
            Resource[] files;
            try {
                files = new PathMatchingResourcePatternResolver().getResources(SEED_IMAGES);
            } catch (IOException exception) {
                log.warn("No seed images found at {}: {}", SEED_IMAGES, exception.getMessage());
                return keys;
            }
            for (Resource file : files) {
                String fileName = file.getFilename();
                if (fileName == null) {
                    continue;
                }
                try (InputStream content = file.getInputStream()) {
                    keys.put(fileName, mediaService.storeSeedImage(fileName, content.readAllBytes()));
                } catch (IOException | RuntimeException exception) {
                    log.warn("Seed image {} could not be published: {}", fileName, exception.getMessage());
                }
            }
            log.info("Published {} of {} seed images to object storage", keys.size(), files.length);
            return keys;
        };
    }

    /**
     * Every seeded gallery, whatever kind of place owns it: attractions, stays, food places, activities and
     * corridors, each item with its caption (the photographer's credit included) and the one that is
     * the cover marked so.
     */
    @Bean
    CatalogSeedContributor mediaGallerySeed(MediaRepository mediaRepository, ObjectMapper objectMapper) {
        return new CatalogSeedContributor() {
            @Override
            public int getOrder() {
                return DETAILS;
            }

            @Override
            public boolean alreadySeeded() {
                return mediaRepository.count() > 0;
            }

            @Override
            public void seed(CatalogSeedContext context) {
                int written = 0;
                for (JsonNode gallery : SeedJson.rows(objectMapper, MinioConfig.class, "galleries.json")) {
                    MediaType ownerType = SeedJson.enumValue(gallery, "ownerType", MediaType.class);
                    String owner = SeedJson.text(gallery, "owner");
                    UUID ownerId = ownerOf(context, ownerType, owner);
                    if (ownerId == null) {
                        log.warn("Gallery for unknown {} {}", ownerType, owner);
                        continue;
                    }
                    for (JsonNode item : SeedJson.array(gallery, "media")) {
                        String key = context.imageKey(item);
                        if (key == null) {
                            continue;
                        }
                        Integer sortOrder = SeedJson.integer(item, "sortOrder");
                        Media media = new Media(ownerType, ownerId, key, sortOrder == null ? 0 : sortOrder);
                        media.setCaption(SeedJson.text(item, "caption"));
                        media.setIsCover(item.path("cover").asBoolean(false));
                        mediaRepository.save(media);
                        written += 1;
                    }
                }
                log.info("Seeded {} gallery items", written);
            }

            @Override
            public void wipe() {
                mediaRepository.deleteAllInBatch();
            }

            private UUID ownerOf(CatalogSeedContext context, MediaType ownerType, String owner) {
                if (ownerType == null || owner == null) {
                    return null;
                }
                return switch (ownerType) {
                    case DESTINATION -> idOf(context.find(Destination.class, owner), Destination::getId);
                    case STAY -> idOf(context.find(Stay.class, owner), Stay::getId);
                    case FOOD -> idOf(context.find(Food.class, owner), Food::getId);
                    case ACTIVITY -> idOf(context.find(Activity.class, owner), Activity::getId);
                    case CORRIDOR -> idOf(context.find(Corridor.class, owner), Corridor::getId);
                    default -> null;
                };
            }

            private <T> UUID idOf(T row, java.util.function.Function<T, UUID> id) {
                return row == null ? null : id.apply(row);
            }
        };
    }
}
