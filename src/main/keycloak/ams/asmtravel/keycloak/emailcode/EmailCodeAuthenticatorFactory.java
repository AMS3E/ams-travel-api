package ams.asmtravel.keycloak.emailcode;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/** Registers the email-code step as {@code ams-email-code}; the Dockerfile writes the service file that names this class. */
public class EmailCodeAuthenticatorFactory implements AuthenticatorFactory {

    public static final String ID = "ams-email-code";

    private static final EmailCodeAuthenticator SINGLETON = new EmailCodeAuthenticator();

    private static final List<ProviderConfigProperty> PROPERTIES = List.of(
            property(EmailCodeAuthenticator.Settings.TTL, "Code lifetime (minutes)", "How long a code stays valid. Five unless set.", "5"),
            property(EmailCodeAuthenticator.Settings.RESEND, "Resend cool-down (seconds)", "How long before another code can be asked for. Sixty unless set.", "60"),
            property(EmailCodeAuthenticator.Settings.ATTEMPTS, "Wrong tries allowed", "After this many wrong codes the sign-in starts over. Five unless set.", "5"));

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayType() {
        return "AMS email code";
    }

    @Override
    public String getHelpText() {
        return "Sends a six-digit code to the account's email address and asks for it. A correct code marks the email verified.";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED};
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return PROPERTIES;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // Nothing to read at boot.
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to wire.
    }

    @Override
    public void close() {
        // Nothing held.
    }

    private static ProviderConfigProperty property(String name, String label, String help, String fallback) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name);
        property.setLabel(label);
        property.setHelpText(help);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(fallback);
        return property;
    }
}
