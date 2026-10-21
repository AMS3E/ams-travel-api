package ams.asmtravel.api.services.activity.stamp.controller;

import ams.asmtravel.api.services.activity.stamp.domain.StampTarget;
import ams.asmtravel.api.services.activity.stamp.dto.request.StampCodeRequest;
import ams.asmtravel.api.services.activity.stamp.dto.request.StampRequest;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampCodeResponse;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampResponse;
import ams.asmtravel.api.services.activity.stamp.service.StampService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Digital stamps: on-site check-in by QR code, collected stamps, and the codes the CMS prints. */
@Tag(name = "Engagement · Digital stamps", description = "Checking in on site with the posted code, the stamps collected, and the codes.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stamps")
public class StampController {

    private final StampService stampService;
    private final UserService userService;

    /** Checks the caller in with the code scanned at a destination of any kind, to collect its stamp. */
    @PostMapping
    public StampResponse collect(Authentication authentication, @Valid @RequestBody StampRequest request) {
        try {
            return stampService.collect(userService.requireUserId(authentication), request);
        } catch (DataIntegrityViolationException raced) {
            // Two check-ins raced to the same stamp; the other one got it.
            return stampService.alreadyCollected(request);
        }
    }

    @GetMapping("/me")
    public List<StampResponse> findMine(Authentication authentication) {
        return stampService.findMine(userService.requireUserId(authentication));
    }

    /** A place's codes with their check-in counts, active first: {@code ?targetType=DESTINATION|STAY|FOOD|ACTIVITY&targetId=}. */
    @GetMapping("/codes")
    public List<StampCodeResponse> getCodes(@RequestParam StampTarget targetType, @RequestParam UUID targetId) {
        return stampService.findCodes(targetType, targetId);
    }

    /** Issues a place's code beside the ones already posted, which keep working. */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/codes")
    public StampCodeResponse issueCode(Authentication authentication, @Valid @RequestBody StampCodeRequest request) {
        return stampService.issueCode(request, userService.requireUserId(authentication));
    }

    /** Retires a print deliberately: it stops scanning while the place's other codes keep going. */
    @PostMapping("/codes/{id}/retire")
    public StampCodeResponse retireCode(Authentication authentication, @PathVariable UUID id) {
        return stampService.retireCode(id, userService.requireUserId(authentication));
    }

    /** Deletes a code issued by mistake and never posted; 409 once it has collected check-ins (retire it instead). */
    @DeleteMapping("/codes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdrawCode(@PathVariable UUID id) {
        stampService.withdrawCode(id);
    }

    /**
     * What a phone camera opens when it reads a printed code outside the app: a plain page saying to scan it in
     * the AMS Travel app. The app itself reads the code off the link's last part.
     */
    @Hidden
    @GetMapping(value = "/scan/{code}", produces = MediaType.TEXT_HTML_VALUE)
    public String scanPage(@PathVariable String code) {
        String place = stampService.placeNameOf(code).map(StampController::escape).orElse(null);
        String title = place == null ? "This check-in code is no longer valid" : "Check in at " + place;
        String body = place == null
                ? "Ask the staff on site for the current code."
                : "Open the AMS Travel app, go to the place's page, tap Check in and scan this code there.";
        return "<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + title + " · AMS Travel</title>\n"
                + "<style>body{margin:0;background:#f6f4ef;color:#1d1d1b;font:16px/1.5 -apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;"
                + "display:flex;min-height:100vh;align-items:center;justify-content:center;padding:24px 16px;box-sizing:border-box}"
                + "main{background:#fff;border-radius:20px;padding:32px 24px;max-width:420px;width:100%;text-align:center;box-shadow:0 12px 40px rgba(0,0,0,.08)}"
                + "h1{font-size:22px;margin:0 0 8px}p{margin:0;color:#6b6a66}footer{margin-top:24px;font-size:13px;color:#6b6a66}</style>\n"
                + "</head>\n<body>\n<main>\n<h1>" + title + "</h1>\n<p>" + body + "</p>\n"
                + "<footer>AMS Travel, Cambodia's travel app by AMS3E</footer>\n</main>\n</body>\n</html>\n";
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
