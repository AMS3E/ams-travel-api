package ams.asmtravel.api.services.authorization.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Permission authorities ({@code ROLE_}-prefixed) and which desk carries which. */
public final class Permissions {

    public static final String FULL_CONTROL = "ROLE_PERM_FULL_CONTROL";
    public static final String AUDIT_VIEW = "ROLE_PERM_AUDIT_VIEW";
    public static final String USER_MANAGE = "ROLE_PERM_USER_MANAGE";
    public static final String ROLE_ASSIGN = "ROLE_PERM_ROLE_ASSIGN";
    public static final String REVIEW_MODERATE = "ROLE_PERM_REVIEW_MODERATE";
    public static final String DESTINATION_DRAFT_CREATE = "ROLE_PERM_DESTINATION_DRAFT_CREATE";
    public static final String LISTING_EDIT_OWN = "ROLE_PERM_LISTING_EDIT_OWN";
    public static final String DESTINATION_DRAFT_EDIT_OWN = "ROLE_PERM_DESTINATION_DRAFT_EDIT_OWN";
    public static final String DESTINATION_EDIT_ANY = "ROLE_PERM_DESTINATION_EDIT_ANY";
    public static final String DESTINATION_DELETE = "ROLE_PERM_DESTINATION_DELETE";
    public static final String LISTING_DELETE_OWN = "ROLE_PERM_LISTING_DELETE_OWN";
    public static final String DESTINATION_DRAFT_DELETE_OWN = "ROLE_PERM_DESTINATION_DRAFT_DELETE_OWN";
    public static final String PARTNER_LISTING_EDIT = "ROLE_PERM_PARTNER_LISTING_EDIT";
    public static final String PARTNER_LISTING_DELETE = "ROLE_PERM_PARTNER_LISTING_DELETE";
    public static final String DESTINATION_PUBLISH = "ROLE_PERM_DESTINATION_PUBLISH";
    public static final String DESTINATION_UNPUBLISH = "ROLE_PERM_DESTINATION_UNPUBLISH";
    public static final String MEDIA_UPLOAD = "ROLE_PERM_MEDIA_UPLOAD";
    public static final String CORRIDOR_CURATE = "ROLE_PERM_CORRIDOR_CURATE";
    public static final String TAXONOMY_MANAGE = "ROLE_PERM_TAXONOMY_MANAGE";
    public static final String PUBLISH_REQUEST_SUBMIT = "ROLE_PERM_PUBLISH_REQUEST_SUBMIT";
    public static final String PUBLISH_REQUEST_VIEW_OWN = "ROLE_PERM_PUBLISH_REQUEST_VIEW_OWN";
    public static final String PARTNER_PUBLISH_APPROVE = "ROLE_PERM_PARTNER_PUBLISH_APPROVE";
    public static final String CONTRIBUTOR_PUBLISH_APPROVE = "ROLE_PERM_CONTRIBUTOR_PUBLISH_APPROVE";
    /** Verify destinations. Keeps the old realm role name on purpose. */
    public static final String DESTINATION_VERIFY = "ROLE_PERM_VERIFIED_BADGE_SET";

    /** Every permission above; add new ones here too. */
    public static final List<String> ALL = List.of(
            FULL_CONTROL, AUDIT_VIEW, USER_MANAGE, ROLE_ASSIGN, REVIEW_MODERATE,
            DESTINATION_DRAFT_CREATE, LISTING_EDIT_OWN, DESTINATION_DRAFT_EDIT_OWN, DESTINATION_EDIT_ANY,
            DESTINATION_DELETE, LISTING_DELETE_OWN, DESTINATION_DRAFT_DELETE_OWN,
            PARTNER_LISTING_EDIT, PARTNER_LISTING_DELETE, DESTINATION_PUBLISH, DESTINATION_UNPUBLISH,
            MEDIA_UPLOAD, CORRIDOR_CURATE, TAXONOMY_MANAGE,
            PUBLISH_REQUEST_SUBMIT, PUBLISH_REQUEST_VIEW_OWN,
            PARTNER_PUBLISH_APPROVE, CONTRIBUTOR_PUBLISH_APPROVE, DESTINATION_VERIFY);

    /** The five desk roles, lowest first. */
    public static final String PARTNER = "PARTNER";
    public static final String CONTRIBUTOR = "CONTRIBUTOR";
    public static final String EDITOR = "EDITOR";
    public static final String BDO = "BDO";
    public static final String ADMIN = "ADMIN";

    /** Permissions each desk carries (Keycloak composites). Rules check permissions, not desks. */
    private static final Map<String, List<String>> DESKS = desks();

    private static Map<String, List<String>> desks() {
        Map<String, List<String>> carried = new LinkedHashMap<>();

        /* Partner: own listings only. */
        carried.put(PARTNER, List.of(
                LISTING_EDIT_OWN, LISTING_DELETE_OWN, MEDIA_UPLOAD,
                PUBLISH_REQUEST_SUBMIT, PUBLISH_REQUEST_VIEW_OWN));

        /* Contributor: partner plus destination drafts. */
        carried.put(CONTRIBUTOR, List.of(
                LISTING_EDIT_OWN, LISTING_DELETE_OWN, MEDIA_UPLOAD,
                PUBLISH_REQUEST_SUBMIT, PUBLISH_REQUEST_VIEW_OWN,
                DESTINATION_DRAFT_CREATE, DESTINATION_DRAFT_EDIT_OWN, DESTINATION_DRAFT_DELETE_OWN));

        /* Editor: reviews contributors; the only desk besides ADMIN that unpublishes. */
        carried.put(EDITOR, List.of(
                MEDIA_UPLOAD, PUBLISH_REQUEST_SUBMIT, PUBLISH_REQUEST_VIEW_OWN,
                DESTINATION_DRAFT_CREATE, DESTINATION_EDIT_ANY, DESTINATION_DELETE,
                DESTINATION_PUBLISH, DESTINATION_UNPUBLISH,
                CONTRIBUTOR_PUBLISH_APPROVE, REVIEW_MODERATE, CORRIDOR_CURATE, TAXONOMY_MANAGE));

        /* BDO: looks after partners and verifies (only BDO and ADMIN verify). */
        carried.put(BDO, List.of(
                MEDIA_UPLOAD, PUBLISH_REQUEST_SUBMIT, PUBLISH_REQUEST_VIEW_OWN,
                DESTINATION_DRAFT_CREATE, DESTINATION_DRAFT_EDIT_OWN, DESTINATION_DRAFT_DELETE_OWN,
                DESTINATION_EDIT_ANY, DESTINATION_PUBLISH,
                PARTNER_LISTING_EDIT, PARTNER_LISTING_DELETE, PARTNER_PUBLISH_APPROVE,
                CORRIDOR_CURATE, TAXONOMY_MANAGE, DESTINATION_VERIFY));

        /* Admin: all four desks plus admin-only permissions. */
        carried.put(ADMIN, List.of(
                "ROLE_" + PARTNER, "ROLE_" + CONTRIBUTOR, "ROLE_" + EDITOR, "ROLE_" + BDO,
                FULL_CONTROL, USER_MANAGE, ROLE_ASSIGN, AUDIT_VIEW));

        return Map.copyOf(carried);
    }

    private Permissions() {
    }

    /** The business roles a back-office account holds, for {@code hasAnyRole}. */
    public static String[] backofficeRoles() {
        return new String[] {PARTNER, CONTRIBUTOR, EDITOR, BDO, ADMIN};
    }

    /** {@link #DESKS} as Keycloak realm role names (no {@code ROLE_} prefix). */
    public static Map<String, List<String>> deskComposites() {
        Map<String, List<String>> composites = new LinkedHashMap<>();
        DESKS.forEach((desk, carried) -> composites.put(desk, carried.stream().map(Permissions::realmRole).toList()));
        return Map.copyOf(composites);
    }

    /** All permissions as Keycloak realm role names (no {@code ROLE_} prefix). */
    public static List<String> realmRoles() {
        return ALL.stream().map(Permissions::realmRole).toList();
    }

    private static String realmRole(String authority) {
        return authority.substring("ROLE_".length());
    }
}
