package ams.asmtravel.api.services.media.client;

import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A magic-number table and a path guard: the things worth testing directly and exhaustively. */
class FileFormatAndStorageKeyTests {

    private static final UUID OWNER = UUID.fromString("5f0b7a3e-1c2d-4e5f-8a9b-0c1d2e3f4a5b");

    private static byte[] bytes(int... values) {
        byte[] content = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            content[index] = (byte) values[index];
        }
        return content;
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void detectsEachSupportedFormatFromItsMagicNumber() {
        assertThat(FileFormat.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo(new FileFormat("image/jpeg", "jpg"));
        assertThat(FileFormat.detect(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
                .isEqualTo(new FileFormat("image/png", "png"));
        assertThat(FileFormat.detect(ascii("GIF89a...."))).isEqualTo(new FileFormat("image/gif", "gif"));
        assertThat(FileFormat.detect(ascii("GIF87a....")).contentType()).isEqualTo("image/gif");
        assertThat(FileFormat.detect(ascii("RIFF????WEBPVP8 "))).isEqualTo(new FileFormat("image/webp", "webp"));
        assertThat(FileFormat.detect(ascii("....ftypavif"))).isEqualTo(new FileFormat("image/avif", "avif"));
        assertThat(FileFormat.detect(ascii("....ftypavis")).extension()).isEqualTo("avif");
    }

    /** What the client claimed is irrelevant: a renamed HTML file is refused however convincing its name. */
    @Test
    void refusesContentThatIsNotOneOfTheFiveFormats() {
        for (byte[] content : new byte[][] {
                ascii("<!DOCTYPE html><script>alert(1)</script>"), ascii("%PDF-1.7"), ascii("PK"),
                bytes(), bytes(0xFF, 0xD8), ascii("RIFF"), null}) {
            assertThatThrownBy(() -> FileFormat.detect(content))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only JPEG, PNG, WebP, GIF, and AVIF");
        }
    }

    /** Invoices are usually PDFs, so evidence may be one; nothing shown on the website may. */
    @Test
    void aPdfIsAcceptedAsEvidenceOnly() {
        assertThat(FileFormat.detect(ascii("%PDF-1.7\n%âãÏÓ"), MediaType.VERIFICATION))
                .isEqualTo(new FileFormat("application/pdf", "pdf"));
        assertThat(FileFormat.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0), MediaType.VERIFICATION).contentType())
                .isEqualTo("image/jpeg");
        for (MediaType category : MediaType.values()) {
            if (category.isPrivate()) continue;
            assertThatThrownBy(() -> FileFormat.detect(ascii("%PDF-1.7"), category))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only JPEG, PNG, WebP, GIF, and AVIF");
        }
        assertThatThrownBy(() -> FileFormat.detect(ascii("<!DOCTYPE html>"), MediaType.VERIFICATION))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Evidence must be a PDF");

        assertThat(StorageKey.parse("verification", OWNER.toString(), "invoice-2026.pdf").extension()).isEqualTo("pdf");
        assertThatThrownBy(() -> new StorageKey(MediaType.DESTINATION, OWNER, "invoice-2026.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid media file name");
    }

    @Test
    void buildsTheCategoryOwnerFileKey() {
        assertThat(new StorageKey(MediaType.DESTINATION, OWNER, "81cc7d8a-2ec3-4f79-864e-10da0ca15a4d.jpg").value())
                .isEqualTo("destination/" + OWNER + "/81cc7d8a-2ec3-4f79-864e-10da0ca15a4d.jpg");
        assertThat(new StorageKey(MediaType.GENERAL, StorageKey.SYSTEM, "Angkor-Wat-2.JPG").value())
                .isEqualTo("general/00000000-0000-0000-0000-000000000000/angkor-wat-2.jpg");
    }

    /** Every key the API makes is one a request may send back as avatarKey, coverKey… */
    @Test
    void everyKeyPassesTheCheckRequestsAreValidatedWith() {
        for (MediaType type : MediaType.values()) {
            String upload = new StorageKey(type, UUID.randomUUID(), UUID.randomUUID() + ".webp").value();
            assertThat(MediaKey.Validator.isImageKey(upload)).as(upload).isTrue();
        }
        assertThat(MediaKey.Validator.isImageKey(new StorageKey(MediaType.GENERAL, StorageKey.SYSTEM, "angkor-wat.jpg").value())).isTrue();
        assertThat(MediaKey.Validator.isImageKey("https://elsewhere.test/a.jpg")).isFalse();
        assertThat(MediaKey.Validator.isImageKey("gallery/" + UUID.randomUUID() + "/a.jpg")).isFalse();
        assertThat(MediaKey.Validator.isImageKey("destination/" + UUID.randomUUID() + "/../a.jpg")).isFalse();
    }

    /** Nothing may address an object outside its own folder, whichever way the escape is spelled. */
    @ParameterizedTest
    @ValueSource(strings = {"../../../etc/passwd.jpg", "..%2f..%2fsecret.jpg", "nested/path.jpg", "angkor wat.jpg",
            "angkor.wat.jpg", "angkor-wat.exe", "angkor-wat", "angkor-wat.jpg.html", ""})
    void refusesAnyFileNameThatCouldLeaveItsFolder(String fileName) {
        assertThatThrownBy(() -> new StorageKey(MediaType.DESTINATION, OWNER, fileName))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid media file name");
    }

    @Test
    void refusesAMissingCategoryOrAnOwnerThatIsNotAUuid() {
        assertThatThrownBy(() -> new StorageKey(null, OWNER, "a.jpg")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> new StorageKey(MediaType.GENERAL, null, "a.jpg"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> StorageKey.parse("general", "1", "a.jpg")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> StorageKey.parse("gallery", OWNER.toString(), "a.jpg"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Media not found");
    }

    /** A link names its folder as keys store it; the private one must not be reachable by another spelling. */
    @Test
    void aLinksFolderIsMatchedExactly() {
        assertThat(StorageKey.parse("verification", OWNER.toString(), "a.jpg").type()).isEqualTo(MediaType.VERIFICATION);
        for (String spelling : new String[] {"VERIFICATION", "Verification", " verification"}) {
            assertThatThrownBy(() -> StorageKey.parse(spelling, OWNER.toString(), "a.jpg"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Media not found");
        }
    }

    @Test
    void aKeySplitsIntoTheFilesNameAndExtension() {
        StorageKey key = new StorageKey(MediaType.AVATAR, OWNER, "81cc7d8a-2ec3-4f79-864e-10da0ca15a4d.webp");
        assertThat(key.name()).isEqualTo("81cc7d8a-2ec3-4f79-864e-10da0ca15a4d");
        assertThat(key.extension()).isEqualTo("webp");
        assertThat(MediaType.DESTINATION.folder() + "/").isEqualTo("destination/");
    }
}
