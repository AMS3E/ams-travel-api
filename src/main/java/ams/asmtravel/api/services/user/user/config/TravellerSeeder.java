package ams.asmtravel.api.services.user.user.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.authorization.role.service.UserRoleService;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.domain.UserStatus;
import ams.asmtravel.api.services.user.user.repository.UserRepository;
import ams.asmtravel.api.services.user.user.service.UserService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Seeds the sample travellers whose reviews and browses make the staging catalogue look used.
 * They have no sign-in: their address ends in {@value #SEED_DOMAIN}, which is also how a reset finds them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TravellerSeeder implements CatalogSeedContributor {

    public static final String SEED_DOMAIN = "@seed.ams.test";

    /** The seed's own editorial desk: what the seeded Verified decisions are filed under. */
    public static final String DESK_KEY = "ams-desk";

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return DETAILS;
    }

    @Override
    public boolean alreadySeeded() {
        return userRepository.countByEmailEndingWith(SEED_DOMAIN) > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, TravellerSeeder.class, "travellers.json");
        Instant now = Instant.now();
        for (JsonNode node : rows) {
            User user = new User();
            user.setEmail(SeedJson.text(node, "email"));
            user.setFullName(SeedJson.text(node, "fullName"));
            user.setPhone(SeedJson.text(node, "phone"));
            user.setStatus(UserStatus.ACTIVE);
            String home = SeedJson.text(node, "homeProvinceSlug");
            Province province = home == null ? null : context.find(Province.class, home);
            user.setHomeProvinceId(province == null ? null : province.getId());
            user = userRepository.saveAndFlush(user);
            userRoleService.replace(user, EnumSet.of(RoleCode.USER));
            Integer joinedDaysAgo = SeedJson.integer(node, "joinedDaysAgo");
            if (joinedDaysAgo != null) {
                userRepository.backdate(user.getId(), now.minus(Duration.ofDays(joinedDaysAgo)));
            }
            context.publish(User.class, SeedJson.text(node, "key"), user);
        }
        User desk = new User();
        desk.setEmail("desk" + SEED_DOMAIN);
        desk.setFullName("AMS Travel Editorial Desk");
        desk.setStatus(UserStatus.ACTIVE);
        desk = userRepository.saveAndFlush(desk);
        userRoleService.replace(desk, EnumSet.of(RoleCode.EDITOR));
        context.publish(User.class, DESK_KEY, desk);
        log.info("Seeded {} travellers and the editorial desk", rows.size());
    }

    /** Through the service, so every other service lets go of what the traveller did. */
    @Override
    public void wipe() {
        for (User traveller : userRepository.findByEmailEndingWith(SEED_DOMAIN)) {
            userService.deleteById(traveller.getId());
        }
        userRepository.flush();
    }
}
