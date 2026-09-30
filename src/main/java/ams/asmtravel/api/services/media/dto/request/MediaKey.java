package ams.asmtravel.api.services.media.dto.request;

import ams.asmtravel.api.services.media.domain.MediaType;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;
import java.util.regex.Pattern;

/** The key of an uploaded image ({@code folder/uploaderUuid/file.ext}); null is allowed, empty only with {@link #allowEmpty}. */
@Documented
@Constraint(validatedBy = MediaKey.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MediaKey {

    String message() default "Upload the image first, then send its key";

    /** An empty key means "no picture". */
    boolean allowEmpty() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<MediaKey, String> {

        private static final Pattern IMAGE_FILE = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*\\.(?:jpg|png|webp|gif|avif)$");

        private boolean allowEmpty;

        @Override
        public void initialize(MediaKey annotation) {
            allowEmpty = annotation.allowEmpty();
        }

        @Override
        public boolean isValid(String key, ConstraintValidatorContext context) {
            if (key == null) {
                return true;
            }
            if (key.isEmpty()) {
                return allowEmpty;
            }
            return isImageKey(key);
        }

        /** Checks the shape only, not that the file exists. */
        public static boolean isImageKey(String key) {
            String[] parts = key.split("/", -1);
            return parts.length == 3 && isFolder(parts[0]) && isUuid(parts[1]) && IMAGE_FILE.matcher(parts[2]).matches();
        }

        private static boolean isFolder(String folder) {
            for (MediaType type : MediaType.values()) {
                if (type.folder().equals(folder)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isUuid(String value) {
            try {
                return UUID.fromString(value).toString().equals(value);
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
    }
}
