package ams.asmtravel.api.services.badge.controller;

import ams.asmtravel.api.services.badge.config.BadgeProperties;
import ams.asmtravel.api.services.badge.domain.BadgeStatus;
import ams.asmtravel.api.services.badge.domain.BadgeType;
import ams.asmtravel.api.services.badge.dto.request.CreateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.request.UpdateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.response.BadgeProgressResponse;
import ams.asmtravel.api.services.badge.dto.response.BadgeResponse;
import ams.asmtravel.api.services.badge.dto.response.UserBadgeResponse;
import ams.asmtravel.api.services.badge.service.BadgeService;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Badge definitions, and each person's progress and unlocks per season. */
@Tag(name = "Badges", description = "Definitions, seasonal progress, level upgrades and earned badges.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/badges")
public class BadgeController {

    private static final DateTimeFormatter EARNED_ON = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Phnom_Penh"));

    private final BadgeService badgeService;
    private final UserService userService;
    private final BadgeProperties properties;
    private final MediaService mediaService;

    /** {@code ?type=&status=}; each may be left out. */
    @GetMapping
    public List<BadgeResponse> getAll(@RequestParam(required = false) BadgeType type,
                                      @RequestParam(required = false) BadgeStatus status) {
        return badgeService.getAll(type, status);
    }

    /** The caller's ladders and badges in a season: the active one, or {@code ?seasonId=}. */
    @GetMapping("/me/progress")
    public BadgeProgressResponse getMyProgress(Authentication authentication, @RequestParam(required = false) UUID seasonId) {
        return badgeService.findProgress(userService.requireUserId(authentication), seasonId);
    }

    /** Every badge the caller earned, newest first, across all seasons. */
    @GetMapping("/me/earned")
    public List<UserBadgeResponse> getMyEarned(Authentication authentication) {
        return badgeService.findEarned(userService.requireUserId(authentication));
    }

    /** The caller opened a destination: counted once per place per day towards the browse ladder. */
    @PutMapping("/me/explorations/{destinationId}")
    public BadgeProgressResponse explore(Authentication authentication, @PathVariable UUID destinationId) {
        return badgeService.explore(userService.requireUserId(authentication), destinationId);
    }

    /** One earned badge by its share link: public, for a share page. */
    @GetMapping("/shared/{userBadgeId}")
    public UserBadgeResponse getShared(@PathVariable UUID userBadgeId) {
        return badgeService.findShared(userBadgeId);
    }

    @GetMapping("/{id}")
    public BadgeResponse getById(@PathVariable UUID id) {
        return badgeService.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BadgeResponse create(@Valid @RequestBody CreateBadgeRequest request) {
        return badgeService.create(request);
    }

    @PutMapping("/{id}")
    public BadgeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBadgeRequest request) {
        return badgeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        badgeService.delete(id);
    }

    /**
     * The public page behind "Share to Facebook": an earned badge and its holder, with the Open Graph tags a
     * share reads for its card. HTML on the API's own origin, since no traveller website exists yet; the
     * badge's {@code shareUrl} points here.
     */
    @Hidden
    @GetMapping(value = "/shared/{userBadgeId}/page", produces = MediaType.TEXT_HTML_VALUE)
    public String sharePage(@PathVariable UUID userBadgeId) {
        UserBadgeResponse badge = badgeService.findShared(userBadgeId);
        String holder = badge.holderName() == null || badge.holderName().isBlank() ? "A traveller" : badge.holderName();
        String title = holder + " earned the " + badge.name() + " badge";
        String season = badge.seasonName() == null ? "" : badge.seasonName();
        String earned = badge.earnedAt() == null ? "" : EARNED_ON.format(badge.earnedAt());
        String description = String.join(" · ", nonBlank(season, earned, "AMS Travel, Cambodia"));
        String image = iconUrl(badge.iconKey());
        String level = badge.level() == null ? "" : badge.level().name().charAt(0) + badge.level().name().substring(1).toLowerCase(Locale.ROOT);

        StringBuilder html = new StringBuilder(2_000);
        html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(escape(title)).append(" · AMS Travel</title>\n")
                .append("<meta property=\"og:type\" content=\"website\">\n")
                .append("<meta property=\"og:site_name\" content=\"AMS Travel\">\n")
                .append("<meta property=\"og:title\" content=\"").append(escape(title)).append("\">\n")
                .append("<meta property=\"og:description\" content=\"").append(escape(description)).append("\">\n")
                .append("<meta property=\"og:url\" content=\"").append(escape(properties.shareUrl(userBadgeId))).append("\">\n");
        if (image != null) {
            html.append("<meta property=\"og:image\" content=\"").append(escape(image)).append("\">\n");
        }
        html.append("<meta name=\"twitter:card\" content=\"summary\">\n")
                .append("<style>\n")
                .append(":root{color-scheme:light dark;--bg:#f6f4ef;--card:#fff;--ink:#1d1d1b;--muted:#6b6a66;--accent:#b4531f}\n")
                .append("@media (prefers-color-scheme:dark){:root{--bg:#171614;--card:#23221f;--ink:#f3f1ec;--muted:#a7a49d;--accent:#e58a57}}\n")
                .append("body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.5 -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;display:flex;min-height:100vh;align-items:center;justify-content:center;padding:24px 16px;box-sizing:border-box}\n")
                .append("main{background:var(--card);border-radius:20px;padding:32px 24px;max-width:420px;width:100%;text-align:center;box-shadow:0 12px 40px rgba(0,0,0,.08)}\n")
                .append("img{width:120px;height:120px;object-fit:contain;border-radius:60px;background:var(--bg);margin-bottom:16px}\n")
                .append(".badge{width:120px;height:120px;border-radius:60px;background:var(--accent);margin:0 auto 16px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:48px}\n")
                .append("h1{font-size:22px;margin:0 0 4px}.level{color:var(--accent);font-weight:600;margin:0 0 16px}\n")
                .append("p{margin:0 0 8px;color:var(--muted)}p.holder{color:var(--ink);font-size:18px}\n")
                .append("footer{margin-top:24px;font-size:13px;color:var(--muted)}\n")
                .append("</style>\n</head>\n<body>\n<main>\n");
        if (image != null) {
            html.append("<img src=\"").append(escape(image)).append("\" alt=\"\">\n");
        } else {
            html.append("<div class=\"badge\" aria-hidden=\"true\">★</div>\n");
        }
        html.append("<h1>").append(escape(badge.name())).append("</h1>\n");
        if (!level.isEmpty()) {
            html.append("<p class=\"level\">").append(escape(level)).append("</p>\n");
        }
        html.append("<p class=\"holder\">").append(escape(holder)).append("</p>\n");
        if (!season.isEmpty()) {
            html.append("<p>").append(escape(season)).append("</p>\n");
        }
        if (!earned.isEmpty()) {
            html.append("<p>Earned on ").append(escape(earned)).append("</p>\n");
        }
        html.append("<footer>AMS Travel, Cambodia's travel app by AMS3E</footer>\n</main>\n</body>\n</html>\n");
        return html.toString();
    }

    /** The badge's icon as an absolute address, or null when it has none the web can show. */
    private String iconUrl(String iconKey) {
        if (iconKey == null || iconKey.isBlank() || !iconKey.contains("/")) {
            return null;
        }
        String url = mediaService.url(iconKey);
        if (url == null) {
            return null;
        }
        return url.startsWith("http") ? url : properties.publicUrl() + (url.startsWith("/") ? url : "/" + url);
    }

    private static String[] nonBlank(String... parts) {
        return java.util.Arrays.stream(parts).filter(part -> part != null && !part.isBlank()).toArray(String[]::new);
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
