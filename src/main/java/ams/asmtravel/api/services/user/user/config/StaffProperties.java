package ams.asmtravel.api.services.user.user.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * How much of the staff realm one screen reads from Keycloak at a time.
 *
 * @param roleMemberLimit members of one role to read, for the staff list
 * @param searchLimit     matches of one staff search to read
 */
@Validated
@ConfigurationProperties(prefix = "app.staff")
public record StaffProperties(
        @Min(1) Integer roleMemberLimit,
        @Min(1) Integer searchLimit
) {
    public StaffProperties {
        roleMemberLimit = roleMemberLimit == null ? 200 : roleMemberLimit;
        searchLimit = searchLimit == null ? 20 : searchLimit;
    }
}
