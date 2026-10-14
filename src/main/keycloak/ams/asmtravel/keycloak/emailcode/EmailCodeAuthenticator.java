package ams.asmtravel.keycloak.emailcode;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;

/**
 * The email-code step: a six-digit code goes to the account's email address and the person types it
 * in on Keycloak's own one-time-code page. It runs after the registration form (the app's "Verify OTP"
 * screen) and can run after the password at sign-in. A correct code also marks the email verified.
 *
 * <p>The code, when it was sent, when it expires and how many tries were made live in the authentication
 * session, so a page refresh shows the same code's page again without sending another mail; the
 * "Send a new code" link, {@code ?resend=true} on the step's refresh URL, sends a fresh one after the
 * cool-down. The texts are the login theme's {@code emailCode*} messages.</p>
 */
public class EmailCodeAuthenticator implements Authenticator {

    static final String NOTE_CODE = "ams.emailcode.code";
    static final String NOTE_EXPIRES = "ams.emailcode.expires";
    static final String NOTE_SENT = "ams.emailcode.sent";
    static final String NOTE_ATTEMPTS = "ams.emailcode.attempts";
    static final String FIELD = "otp";
    static final String RESEND = "resend";

    private static final Logger LOG = Logger.getLogger(EmailCodeAuthenticator.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            context.attempted();
            return;
        }
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                    context.form().setError("emailCodeNoEmail").createErrorPage(Response.Status.BAD_REQUEST));
            return;
        }
        Settings settings = Settings.of(context.getAuthenticatorConfig());
        AuthenticationSessionModel session = context.getAuthenticationSession();
        boolean resend = "true".equals(context.getHttpRequest().getUri().getQueryParameters().getFirst(RESEND));
        long now = Instant.now().getEpochSecond();
        String code = session.getAuthNote(NOTE_CODE);
        boolean expired = code == null || now > number(session.getAuthNote(NOTE_EXPIRES));

        if (resend && !expired && now - number(session.getAuthNote(NOTE_SENT)) < settings.resendSeconds) {
            context.challenge(page(context, "emailCodeTooSoon", email, settings, false));
            return;
        }
        if (resend || expired) {
            if (!send(context, user, email, settings)) {
                context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                        context.form().setError("emailCodeSendFailed").createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
                return;
            }
            context.challenge(page(context, resend ? "emailCodeResent" : "emailCodeSent", email, settings, false));
            return;
        }
        context.challenge(page(context, "emailCodeSent", email, settings, false));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();
        if (form.containsKey("cancel")) {
            context.resetFlow();
            return;
        }
        UserModel user = context.getUser();
        String email = user.getEmail();
        Settings settings = Settings.of(context.getAuthenticatorConfig());
        AuthenticationSessionModel session = context.getAuthenticationSession();
        long now = Instant.now().getEpochSecond();
        String code = session.getAuthNote(NOTE_CODE);
        if (code == null || now > number(session.getAuthNote(NOTE_EXPIRES))) {
            send(context, user, email, settings);
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, page(context, "emailCodeExpired", email, settings, true));
            return;
        }
        int attempts = (int) number(session.getAuthNote(NOTE_ATTEMPTS)) + 1;
        session.setAuthNote(NOTE_ATTEMPTS, String.valueOf(attempts));
        String typed = form.getFirst(FIELD);
        if (typed == null || !code.equals(typed.trim())) {
            if (attempts >= settings.maxAttempts) {
                clear(session);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form().setError("emailCodeTooManyAttempts").createErrorPage(Response.Status.UNAUTHORIZED));
                return;
            }
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, page(context, "emailCodeInvalid", email, settings, true));
            return;
        }
        clear(session);
        user.setEmailVerified(true);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Nothing to set up: the email address is all it needs.
    }

    @Override
    public void close() {
        // Nothing held.
    }

    /** A fresh code, remembered in the session and sent by the realm's SMTP server as the AMS Travel mail. False when the mail could not go. */
    private boolean send(AuthenticationFlowContext context, UserModel user, String email, Settings settings) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        AuthenticationSessionModel session = context.getAuthenticationSession();
        long now = Instant.now().getEpochSecond();
        session.setAuthNote(NOTE_CODE, code);
        session.setAuthNote(NOTE_EXPIRES, String.valueOf(now + settings.ttlMinutes * 60L));
        session.setAuthNote(NOTE_SENT, String.valueOf(now));
        session.setAuthNote(NOTE_ATTEMPTS, "0");
        String subject = EmailCodeMessage.subject(code);
        String text = EmailCodeMessage.text(code, settings.ttlMinutes);
        String html = EmailCodeMessage.html(code, settings.ttlMinutes);
        try {
            context.getSession().getProvider(EmailSenderProvider.class)
                    .send(context.getRealm().getSmtpConfig(), user, subject, text, html);
            return true;
        } catch (EmailException problem) {
            LOG.warnf(problem, "Could not send the email code to %s", email);
            clear(session);
            return false;
        }
    }

    /** Keycloak's one-time-code page with the message that fits: info while waiting, error after a wrong code. */
    private static Response page(AuthenticationFlowContext context, String key, String email, Settings settings, boolean error) {
        String resendUrl = UriBuilder.fromUri(context.getRefreshExecutionUrl()).queryParam(RESEND, "true").build().toString();
        LoginFormsProvider form = context.form();
        form = error ? form.setError(key, email, settings.ttlMinutes, resendUrl) : form.setInfo(key, email, settings.ttlMinutes, resendUrl);
        return form.createLoginTotp();
    }

    private static void clear(AuthenticationSessionModel session) {
        session.removeAuthNote(NOTE_CODE);
        session.removeAuthNote(NOTE_EXPIRES);
        session.removeAuthNote(NOTE_SENT);
        session.removeAuthNote(NOTE_ATTEMPTS);
    }

    private static long number(String note) {
        try {
            return note == null ? 0 : Long.parseLong(note);
        } catch (NumberFormatException wrong) {
            return 0;
        }
    }

    /** The step's settings, from the flow's configuration or the defaults: five minutes, one resend a minute, five tries. */
    record Settings(int ttlMinutes, int resendSeconds, int maxAttempts) {
        static final String TTL = "ttlMinutes";
        static final String RESEND = "resendSeconds";
        static final String ATTEMPTS = "maxAttempts";

        static Settings of(AuthenticatorConfigModel model) {
            Map<String, String> config = model == null || model.getConfig() == null ? Map.of() : model.getConfig();
            return new Settings(setting(config, TTL, 5), setting(config, RESEND, 60), setting(config, ATTEMPTS, 5));
        }

        private static int setting(Map<String, String> config, String key, int fallback) {
            try {
                String value = config.get(key);
                return value == null || value.isBlank() ? fallback : Math.max(1, Integer.parseInt(value.trim()));
            } catch (NumberFormatException wrong) {
                return fallback;
            }
        }
    }
}
