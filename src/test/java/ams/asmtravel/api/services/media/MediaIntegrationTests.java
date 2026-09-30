package ams.asmtravel.api.services.media;

import ams.asmtravel.api.services.user.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uploading and deleting pictures, with MinIO stubbed. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class MediaIntegrationTests {

    private static final String BASE = "http://localhost:8080/api/v1/media/files/";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @MockitoBean MinioClient minioClient;

    @BeforeEach
    void setup() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
    }

    /** The owner segment is the uploader as the token says, never what the request claims. */
    @Test
    void aBatchKeepsItsOrderAndIsFiledUnderTheUploader() throws Exception {
        var response = mvc.perform(multipart("/api/v1/media/files/batch")
                        .file(png("files", "first.png")).file(png("files", "second.png"))
                        .param("type", "destination").param("owner", "someone-else")
                        .with(caller("ADMIN", "PERM_FULL_CONTROL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].mimeTypeFile").value("image/png"))
                .andExpect(jsonPath("$[0].category").value("destination"))
                .andExpect(jsonPath("$[1].extension").value("png"));

        String ownerId = users.findByEmailIgnoreCase("uploader@media.test").orElseThrow().getId().toString();
        response.andExpect(jsonPath("$[0].key", startsWith("destination/" + ownerId + "/")))
                .andExpect(jsonPath("$[1].uri", startsWith(BASE + "destination/" + ownerId + "/")));
    }

    @Test
    void anUploadAnswersWithItsKeyAndLink() throws Exception {
        mvc.perform(multipart("/api/v1/media/files").file(png("file", "single.png")).with(caller("PERM_FULL_CONTROL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key", matchesPattern("general/[0-9a-f-]{36}/[0-9a-f-]{36}\\.png")))
                .andExpect(jsonPath("$.name", matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.extension").value("png"))
                .andExpect(jsonPath("$.uri", startsWith(BASE + "general/")))
                .andExpect(jsonPath("$.size").value(9))
                .andExpect(jsonPath("$.category").value("general"));
    }

    @Test
    void uploadingNeedsASignIn() throws Exception {
        mvc.perform(multipart("/api/v1/media/files/batch").file(png("files", "image.png")))
                .andExpect(status().isUnauthorized());
        verify(minioClient, never()).putObject(any());
    }

    /** Catalogue media needs PERM_MEDIA_UPLOAD; a role alone is not a grant. */
    @Test
    void catalogueMediaNeedsAnExplicitGrant() throws Exception {
        for (String role : new String[] {"USER", "PARTNER", "CONTRIBUTOR", "EDITOR", "BDO", "ADMIN"}) {
            mvc.perform(multipart("/api/v1/media/files").file(png("file", "image.png"))
                            .param("type", "destination").with(caller(role)))
                    .andExpect(status().isForbidden());
        }
        verify(minioClient, never()).putObject(any());

        mvc.perform(multipart("/api/v1/media/files").file(png("file", "image.png"))
                        .param("type", "food").with(caller("PARTNER", "PERM_MEDIA_UPLOAD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("food"));
    }

    @Test
    void avatarsAndReviewPhotosAreAnyonesToUpload() throws Exception {
        mvc.perform(multipart("/api/v1/media/files").file(png("file", "me.png")).param("type", "avatar")
                        .with(caller("USER")))
                .andExpect(status().isCreated());
        mvc.perform(multipart("/api/v1/media/files/batch").file(png("files", "dish.png")).param("type", "review")
                        .with(caller("USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].category").value("review"));
    }

    /** An invoice arrives as a PDF more often than as a photo: evidence may be one, a catalogue picture may not. */
    @Test
    void aPdfInvoiceIsEvidenceAndNothingElse() throws Exception {
        MockMultipartFile invoice = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "%PDF-1.7\n1 0 obj".getBytes());
        mvc.perform(multipart("/api/v1/media/files").file(invoice).param("type", "verification")
                        .with(caller("EDITOR", "PERM_VERIFIED_BADGE_SET")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key", matchesPattern("verification/[0-9a-f-]{36}/[0-9a-f-]{36}\\.pdf")))
                .andExpect(jsonPath("$.mimeTypeFile").value("application/pdf"));
        mvc.perform(multipart("/api/v1/media/files").file(invoice).param("type", "destination")
                        .with(caller("PERM_FULL_CONTROL")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingFilesUnknownFoldersAndFilesThatAreNotImagesAreRefused() throws Exception {
        mvc.perform(multipart("/api/v1/media/files/batch").with(caller("PERM_FULL_CONTROL")))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/media/files").file(png("file", "image.png")).param("type", "gallery")
                        .with(caller("PERM_FULL_CONTROL")))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/media/files/batch").file(png("files", "image.png"))
                        .file(new MockMultipartFile("files", "invalid.png", "image/png", "invalid".getBytes()))
                        .with(caller("PERM_FULL_CONTROL")))
                .andExpect(status().isBadRequest());
        verify(minioClient, never()).putObject(any());
    }

    /** A file is its uploader's to delete, or full control's; deleting removes it from the bucket. */
    @Test
    void aFileIsDeletedByItsUploaderOnly() throws Exception {
        String key = upload(caller("USER"), "avatar");
        String link = "/api/v1/media/files/" + key;

        mvc.perform(delete(link)).andExpect(status().isUnauthorized());
        mvc.perform(delete(link).with(as("other@media.test", "USER"))).andExpect(status().isForbidden());
        verify(minioClient, never()).removeObject(any());

        mvc.perform(delete(link).with(caller("USER"))).andExpect(status().isNoContent());
        ArgumentCaptor<RemoveObjectArgs> removed = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(removed.capture());
        assertThat(removed.getValue().object()).isEqualTo(key);

        mvc.perform(delete("/api/v1/media/files/" + upload(caller("USER"), "avatar"))
                        .with(as("admin@media.test", "ADMIN", "PERM_FULL_CONTROL")))
                .andExpect(status().isNoContent());
    }

    @Test
    void aLinkThatIsNotAKeyIsABadRequest() throws Exception {
        mvc.perform(delete("/api/v1/media/files/avatar/1/photo.png").with(caller("USER")))
                .andExpect(status().isBadRequest());
    }

    /* ---------------------------------------------------------------------- */

    private String upload(RequestPostProcessor caller, String category) throws Exception {
        String body = mvc.perform(multipart("/api/v1/media/files").file(png("file", "mine.png"))
                        .param("type", category).with(caller))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private RequestPostProcessor caller(String... grants) {
        return as("uploader@media.test", grants);
    }

    private RequestPostProcessor as(String email, String... grants) {
        return jwt().jwt(token -> token.subject("subject-" + email).claim("email", email))
                .authorities(Arrays.stream(grants)
                        .map(grant -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + grant))
                        .toList());
    }

    private static MockMultipartFile png(String part, String name) {
        return new MockMultipartFile(part, name, "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0});
    }
}
