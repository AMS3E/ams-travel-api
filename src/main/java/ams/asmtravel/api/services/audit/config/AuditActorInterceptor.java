package ams.asmtravel.api.services.audit.config;

import ams.asmtravel.api.services.user.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Resolves the signed-in writer before a write starts.
 * The audit listener runs inside a Hibernate flush and must not query.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditActorInterceptor implements HandlerInterceptor {

    private static final Set<String> READS = Set.of("GET", "HEAD", "OPTIONS");

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!READS.contains(request.getMethod()) && authentication instanceof JwtAuthenticationToken) {
            try {
                userService.findUserId(authentication);
            } catch (RuntimeException unresolved) {
                log.debug("Audit: could not resolve the caller of {} {}", request.getMethod(), request.getRequestURI(), unresolved);
            }
        }
        return true;
    }
}
