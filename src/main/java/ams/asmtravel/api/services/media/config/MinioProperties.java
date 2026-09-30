package ams.asmtravel.api.services.media.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/** {@code media.*}: bucket, MinIO connection, max upload size and {@code baseUri} for file links. */
@Validated
@ConfigurationProperties(prefix = "media")
public record MinioProperties(
        @NotBlank String bucket,
        @NotBlank String baseUri,
        @NotNull DataSize maxFileSize,
        @Valid @NotNull Connection minio
) {
    public MinioProperties {
        baseUri = baseUri == null ? null : baseUri.replaceAll("/+$", "");
    }

    public record Connection(
            @NotBlank String endpoint,
            @NotBlank String accessKey,
            @NotBlank String secretKey
    ) {
    }
}
