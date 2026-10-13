package ams.asmtravel.keycloak.emailcode;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * After the password: emails a 6-digit code and asks for it on Keycloak's own
 * one-time-code page (login-otp.ftl), so no template of ours is needed.
 */
public class EmailCodeAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(EmailCodeAuthenticator.class);

    static final int CODE_LENGTH = 6;
    static final int CODE_TTL_SECONDS = 300;
    static final int MAX_ATTEMPTS = 5;
    static final int RESEND_AFTER_SECONDS = 60;

    /** The form field of Keycloak's one-time-code page. */
    private static final String CODE_FIELD = "otp";
    private static final String RESEND_PARAM = "resend";

    private static final String NOTE_HASH = "email-code.hash";
    private static final String NOTE_EXPIRES = "email-code.expires";
    private static final String NOTE_SENT = "email-code.sent";
    private static final String NOTE_ATTEMPTS = "email-code.attempts";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    context.form().setError("emailCodeNoEmail").createErrorPage(Response.Status.BAD_REQUEST));
            return;
        }
        AuthenticationSessionModel auth = context.getAuthenticationSession();
        boolean resend = context.getHttpRequest().getUri().getQueryParameters().containsKey(RESEND_PARAM);

        // A page refresh keeps the live code; "Send a new code" replaces it, at most once a minute.
        if (hasLiveCode(auth) && !resend) {
            context.challenge(page(context, "emailCodeSent"));
        } else if (resend && hasLiveCode(auth) && secondsSinceSent(auth) < RESEND_AFTER_SECONDS) {
            context.challenge(page(context, "emailCodeTooSoon"));
        } else if (send(context)) {
            context.challenge(page(context, resend ? "emailCodeResent" : "emailCodeSent"));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();
        AuthenticationSessionModel auth = context.getAuthenticationSession();
        String entered = form.getFirst(CODE_FIELD) == null ? "" : form.getFirst(CODE_FIELD).replaceAll("\\s", "");
        String expected = auth.getAuthNote(NOTE_HASH);

        if (expected == null || now() > number(auth.getAuthNote(NOTE_EXPIRES))) {
            // An expired code is replaced right away, so there is always one that works.
            if (send(context)) {
                context.challenge(page(context, "emailCodeExpired"));
            }
            return;
        }
        if (!MessageDigest.isEqual(hash(auth, entered), expected.getBytes(StandardCharsets.UTF_8))) {
            long attempts = number(auth.getAuthNote(NOTE_ATTEMPTS)) + 1;
            if (attempts >= MAX_ATTEMPTS) {
                forget(auth);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form().setError("emailCodeTooManyAttempts").createErrorPage(Response.Status.BAD_REQUEST));
                return;
            }
            auth.setAuthNote(NOTE_ATTEMPTS, String.valueOf(attempts));
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, page(context, "emailCodeInvalid", true));
            return;
        }

        forget(auth);
        UserModel user = context.getUser();
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
        }
        context.success();
    }

    private Response page(AuthenticationFlowContext context, String messageKey) {
        return page(context, messageKey, false);
    }

    /** Keycloak's one-time-code page; its one message line always carries a "Send a new code" link. */
    private Response page(AuthenticationFlowContext context, String messageKey, boolean error) {
        URI resend = context.getRefreshExecutionUrl();
        String link = resend + (resend.getQuery() == null ? "?" : "&") + RESEND_PARAM + "=true";
        Object[] parameters = {mask(context.getUser().getEmail()), CODE_TTL_SECONDS / 60, link};
        LoginFormsProvider form = context.form();
        if (error) {
            form.setError(messageKey, parameters);
        } else {
            form.setInfo(messageKey, parameters);
        }
        return form.createLoginTotp();
    }

    private boolean send(AuthenticationFlowContext context) {
        String code = code();
        AuthenticationSessionModel auth = context.getAuthenticationSession();
        auth.setAuthNote(NOTE_HASH, new String(hash(auth, code), StandardCharsets.UTF_8));
        auth.setAuthNote(NOTE_EXPIRES, String.valueOf(now() + CODE_TTL_SECONDS));
        auth.setAuthNote(NOTE_SENT, String.valueOf(now()));
        auth.setAuthNote(NOTE_ATTEMPTS, "0");

        RealmModel realm = context.getRealm();
        try {
            context.getSession().getProvider(EmailSenderProvider.class).send(realm.getSmtpConfig(), context.getUser(),
                    EmailCodeMessage.SUBJECT, EmailCodeMessage.text(code), EmailCodeMessage.html(code));
            return true;
        } catch (EmailException exception) {
            forget(auth);
            LOG.errorf(exception, "Could not email a sign-in code to user %s in realm %s", context.getUser().getId(), realm.getName());
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("emailCodeSendFailed").createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return false;
        }
    }

    private static boolean hasLiveCode(AuthenticationSessionModel auth) {
        return auth.getAuthNote(NOTE_HASH) != null && now() <= number(auth.getAuthNote(NOTE_EXPIRES));
    }

    private static long secondsSinceSent(AuthenticationSessionModel auth) {
        return now() - number(auth.getAuthNote(NOTE_SENT));
    }

    private static void forget(AuthenticationSessionModel auth) {
        auth.removeAuthNote(NOTE_HASH);
        auth.removeAuthNote(NOTE_EXPIRES);
        auth.removeAuthNote(NOTE_ATTEMPTS);
    }

    private static String code() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    // Only a hash tied to this sign-in tab is kept, never the code.
    private static byte[] hash(AuthenticationSessionModel auth, String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (auth.getParentSession().getId() + ":" + auth.getTabId() + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).getBytes(StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? email : email.charAt(0) + "*".repeat(Math.min(at - 1, 6)) + email.substring(at);
    }

    private static long number(String value) {
        try {
            return value == null ? 0L : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getEmail() != null && !user.getEmail().isBlank();
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
