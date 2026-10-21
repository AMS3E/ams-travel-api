package ams.asmtravel.api.services.media.config;

import ams.asmtravel.api.services.admin.domain.SeedImages;
import ams.asmtravel.api.services.media.client.MinioStorageClient;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import ams.asmtravel.api.services.media.mapper.MediaMapperImpl;
import ams.asmtravel.api.services.media.repository.MediaRepository;
import ams.asmtravel.api.services.media.service.impl.MediaServiceImpl;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Against the real service: the seed photos' keys must be the ones the catalogue seed files name. */
class MinioConfigTests {

    private SeedImages seedImages;

    @BeforeEach
    void setUp() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any())).thenReturn(true);
        // Built before the stubbing below: Mockito rejects a nested when().
        var missing = missingObject();
        when(minioClient.statObject(any())).thenThrow(missing);

        MinioProperties properties = new MinioProperties(
                "ams-travel-images",
                "http://localhost:8080/api/v1/media/files",
                DataSize.ofMegabytes(20),
                new MinioProperties.Connection("http://localhost:9000", "access-key", "secret-key"));
        MediaRepository repository = mock(MediaRepository.class);
        MediaServiceImpl mediaService = new MediaServiceImpl(new MinioStorageClient(minioClient, properties), properties,
                repository, new MediaMapperImpl(), null, null, null, null, null);
        seedImages = new MinioConfig().seedImages(mediaService);
    }

    @Test
    void everySeedPhotographIsFiledUnderTheSystemOwner() {
        Map<String, String> keys = seedImages.publish();

        assertThat(keys).hasSizeGreaterThanOrEqualTo(51);
        assertThat(keys.get("angkor-wat.jpg"))
                .isEqualTo("general/00000000-0000-0000-0000-000000000000/angkor-wat.jpg");
        assertThat(keys).allSatisfy((fileName, key) ->
                assertThat(key).isEqualTo("general/00000000-0000-0000-0000-000000000000/" + fileName)
                        .matches(MediaKey.Validator::isImageKey, "an image key"));
    }

    private ErrorResponseException missingObject() {
        var error = mock(ErrorResponse.class);
        when(error.code()).thenReturn("NoSuchKey");
        var exception = mock(ErrorResponseException.class);
        when(exception.errorResponse()).thenReturn(error);
        return exception;
    }
}
