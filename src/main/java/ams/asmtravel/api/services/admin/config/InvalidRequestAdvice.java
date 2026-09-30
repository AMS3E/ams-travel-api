package ams.asmtravel.api.services.admin.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Answers invalid request bodies with each bad field and what to change, so a form can mark them. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class InvalidRequestAdvice {

    /** Constraints whose default sentence is better said as "is required". */
    private static final Set<String> REQUIRED = Set.of("NotNull", "NotBlank", "NotEmpty");
    private static final Pattern INDEXED = Pattern.compile("^(\\w+)\\[([^\\]]+)]$");

    /** One refused field: its request name, a readable label, and what to change. */
    public record FieldProblem(String field, String label, String message) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException refused, HttpServletRequest request) {
        List<FieldProblem> problems = new ArrayList<>();
        for (FieldError error : refused.getBindingResult().getFieldErrors()) {
            problems.add(new FieldProblem(error.getField(), label(error.getField()), message(error)));
        }
        for (ObjectError error : refused.getBindingResult().getGlobalErrors()) {
            problems.add(new FieldProblem(null, null, message(error)));
        }

        String detail = problems.isEmpty()
                ? "The request could not be read. Check the fields and try again."
                : String.join(" ", problems.stream().map(InvalidRequestAdvice::sentence).toList());
        log.warn("Refused {} {}: {}", request.getMethod(), request.getRequestURI(), detail);

        ProblemDetail answer = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        answer.setInstance(URI.create(request.getRequestURI()));
        answer.setProperty("errors", problems);
        return answer;
    }

    /** A body Jackson could not read (wrong value type or broken JSON): names the field and what it expects. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableBody(HttpMessageNotReadableException refused, HttpServletRequest request) {
        FieldProblem problem = problemIn(refused);
        String detail = problem == null
                ? "The body could not be read. It must be JSON of the shape this endpoint takes."
                : sentence(problem);
        log.warn("Could not read {} {}: {}", request.getMethod(), request.getRequestURI(), detail);

        ProblemDetail answer = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        answer.setInstance(URI.create(request.getRequestURI()));
        answer.setProperty("errors", problem == null ? List.of() : List.of(problem));
        return answer;
    }

    /** The field Jackson stopped at and why; null when it stopped before any field. */
    private static FieldProblem problemIn(HttpMessageNotReadableException refused) {
        if (!(refused.getCause() instanceof JacksonException cause)) {
            return null;
        }
        if (cause instanceof StreamReadException) {
            return new FieldProblem(null, null, "The body is not valid JSON");
        }
        String path = cause instanceof MismatchedInputException mismatch ? pathOf(mismatch) : null;
        if (path == null || path.isBlank()) {
            return new FieldProblem(null, null, "The body is not in the shape this endpoint takes");
        }
        return new FieldProblem(path, label(path), expected(cause));
    }

    /** {@code days[0].stops[1].arrivalTime}, as the validation errors spell it. */
    private static String pathOf(MismatchedInputException mismatch) {
        StringBuilder path = new StringBuilder();
        for (var step : mismatch.getPath()) {
            if (step.getPropertyName() != null) {
                path.append(path.isEmpty() ? "" : ".").append(step.getPropertyName());
            } else if (step.getIndex() >= 0 && !path.isEmpty()) {
                path.append("[").append(step.getIndex()).append("]");
            }
        }
        return path.toString();
    }

    /** What the field expects, in plain words. */
    private static String expected(JacksonException cause) {
        if (!(cause instanceof InvalidFormatException wrongValue)) {
            /* Wrong shape, e.g. a word where a list belongs. */
            Class<?> wanted = cause instanceof MismatchedInputException mismatch ? mismatch.getTargetType() : null;
            if (wanted != null && java.util.Collection.class.isAssignableFrom(wanted)) {
                return "takes a list, such as [\"first\", \"second\"]";
            }
            if (wanted != null && java.util.Map.class.isAssignableFrom(wanted)) {
                return "takes an object, such as {\"key\": \"value\"}";
            }
            return "is not of the right sort";
        }
        String sent = Objects.toString(wrongValue.getValue(), "");
        Class<?> wanted = wrongValue.getTargetType();
        String shape = shapeOf(wanted);
        return "'" + sent + "' " + shape;
    }

    /** How a type reads to a person, with an example where useful. */
    private static String shapeOf(Class<?> wanted) {
        if (wanted != null && wanted.isEnum()) {
            String allowed = String.join(", ", java.util.Arrays.stream(wanted.getEnumConstants()).map(Object::toString).toList());
            return "is not one of " + allowed;
        }
        if (wanted == java.time.LocalTime.class) return "is not a time — use 14:00";
        if (wanted == java.time.LocalDate.class) return "is not a date — use 2026-09-23";
        if (wanted == java.time.Instant.class || wanted == java.time.OffsetDateTime.class) {
            return "is not a moment — use 2026-09-23T08:00:00Z";
        }
        if (wanted == java.util.UUID.class) return "is not an id — use one the API gave you";
        if (wanted == Integer.class || wanted == Long.class || wanted == Short.class) return "is not a whole number";
        if (wanted == java.math.BigDecimal.class || wanted == Double.class || wanted == Float.class) return "is not a number";
        if (wanted == Boolean.class) return "is not true or false";
        return "is not of the right sort";
    }

    /** "Slug: Use lower-case words joined by hyphens." — or the message alone when it names no field of its own. */
    private static String sentence(FieldProblem problem) {
        String message = problem.message().endsWith(".") ? problem.message() : problem.message() + ".";
        return problem.label() == null ? message : problem.label() + ": " + message;
    }

    private static String message(ObjectError error) {
        String code = error.getCode();
        if (code != null && REQUIRED.contains(code)) {
            return "is required";
        }
        String message = error.getDefaultMessage();
        return message == null || message.isBlank() ? "is not valid" : message;
    }

    /** A readable field label: {@code days[0].stops[1].arrivalTime} becomes "Day 1 › Stop 2 › Arrival time". */
    static String label(String path) {
        List<String> parts = new ArrayList<>();
        for (String segment : path.split("\\.")) {
            Matcher indexed = INDEXED.matcher(segment);
            if (indexed.matches() && indexed.group(2).chars().allMatch(Character::isDigit)) {
                String name = words(indexed.group(1));
                String singular = name.endsWith("s") ? name.substring(0, name.length() - 1) : name;
                parts.add(singular + " " + (Integer.parseInt(indexed.group(2)) + 1));
            } else if (indexed.matches()) {
                parts.add(words(indexed.group(1)) + " (" + indexed.group(2) + ")");
            } else {
                parts.add(words(segment));
            }
        }
        return String.join(" › ", parts);
    }

    private static String words(String name) {
        String spaced = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        spaced = spaced.replaceAll(" ids$", "s").replaceAll(" id$", "").replaceAll(" key uploaded$", " key");
        return spaced.isEmpty() ? name : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
