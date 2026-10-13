package ams.asmtravel.api.services.location.region;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static ams.asmtravel.api.support.Callers.staff;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class TaxonomyIntegrationTests {

    @Autowired MockMvc mvc;

    @Test
    void aZoneHoldsProvincesAndCannotBeDeletedWhileItDoes() throws Exception {
        String region = create("/api/v1/regions", "{\"name\":\"South Coast\",\"code\":\"coast\"}");
        mvc.perform(get("/api/v1/regions/" + region))
                .andExpect(jsonPath("$.code").value("COAST"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.parentId").isEmpty());
        mvc.perform(post("/api/v1/regions").with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Again\",\"code\":\"COAST\"}"))
                .andExpect(status().isConflict());

        // A province is asked for by zone and name alone; its address is made from the name.
        String province = create("/api/v1/provinces",
                "{\"regionId\":\"" + region + "\",\"name\":\"Kep Bay\"}");
        mvc.perform(get("/api/v1/provinces/" + province))
                .andExpect(jsonPath("$.regionId").value(region))
                .andExpect(jsonPath("$.slug").value("kep-bay"))
                .andExpect(jsonPath("$.countryCode").value("KH"))
                .andExpect(jsonPath("$.code").doesNotExist());

        // A second one of the same name gets an address of its own, and a rename keeps it and its zone.
        String tbongKhmum = create("/api/v1/provinces", "{\"regionId\":\"" + region + "\",\"name\":\"Kep Bay\"}");
        mvc.perform(get("/api/v1/provinces/" + tbongKhmum)).andExpect(jsonPath("$.slug").value("kep-bay-2"));
        mvc.perform(put("/api/v1/provinces/" + tbongKhmum).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tboung Khmum\"}"))
                .andExpect(jsonPath("$.slug").value("kep-bay-2"))
                .andExpect(jsonPath("$.name").value("Tboung Khmum"))
                .andExpect(jsonPath("$.regionId").value(region));
        mvc.perform(delete("/api/v1/provinces/" + tbongKhmum).with(owner())).andExpect(status().isNoContent());

        // The zone is optional: the create form asks the name alone.
        String zoneless = create("/api/v1/provinces", "{\"name\":\"Pailin Hills\"}");
        mvc.perform(get("/api/v1/provinces/" + zoneless))
                .andExpect(jsonPath("$.regionId").isEmpty())
                .andExpect(jsonPath("$.slug").value("pailin-hills"));
        mvc.perform(delete("/api/v1/provinces/" + zoneless).with(owner())).andExpect(status().isNoContent());

        // A tourism region is a theme: it holds no provinces.
        String theme = create("/api/v1/regions", "{\"name\":\"Coastal & Island\",\"code\":\"COASTAL-1\",\"kind\":\"TOURISM\"}");
        mvc.perform(post("/api/v1/provinces").with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regionId\":\"" + theme + "\",\"name\":\"Koh Kong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Coastal & Island is a tourism region: a province belongs to one of the zones"));

        mvc.perform(delete("/api/v1/regions/" + region).with(owner())).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/provinces/" + province).with(owner())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/regions/" + region).with(owner())).andExpect(status().isNoContent());
    }

    /**
     * The Discover layer's first tab: a tourism region carries the categories
     * its shelves are built from and the tags its filter chips offer, is asked
     * for by kind, and leaves its links alone when a caller saves without them.
     */
    @Test
    void aTourismRegionCarriesItsShelvesAndItsFilters() throws Exception {
        String category = create("/api/v1/categories", "{\"name\":\"Temples\"}");
        String tag = create("/api/v1/tags", "{\"name\":\"UNESCO\"}");
        String interest = create("/api/v1/interests", "{\"name\":\"Historical Sites\",\"groupName\":\"Attraction Sites\"}");
        String region = create("/api/v1/regions",
                "{\"name\":\"Ancient Capitals\",\"code\":\"ANCIENT-1\",\"kind\":\"TOURISM\","
                        + "\"categoryIds\":[\"" + category + "\"],\"tagIds\":[\"" + tag + "\"],"
                        + "\"interestIds\":[\"" + interest + "\"]}");
        mvc.perform(get("/api/v1/regions/" + region))
                .andExpect(jsonPath("$.kind").value("TOURISM"))
                .andExpect(jsonPath("$.categoryIds", hasSize(1)))
                .andExpect(jsonPath("$.categoryIds[0]").value(category))
                .andExpect(jsonPath("$.tagIds[0]").value(tag))
                .andExpect(jsonPath("$.interestIds[0]").value(interest));

        String zone = create("/api/v1/regions", "{\"name\":\"Tonle Sap\",\"code\":\"TONLE-1\"}");
        mvc.perform(get("/api/v1/regions/" + zone))
                .andExpect(jsonPath("$.kind").value("ZONE"))
                .andExpect(jsonPath("$.categoryIds", hasSize(0)));

        mvc.perform(get("/api/v1/regions").param("kind", "TOURISM"))
                .andExpect(jsonPath("$[?(@.code == 'ANCIENT-1')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.code == 'TONLE-1')]", hasSize(0)));

        // A save that says nothing about the links keeps them.
        mvc.perform(put("/api/v1/regions/" + region).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ancient Capitals\",\"code\":\"ANCIENT-1\"}"))
                .andExpect(jsonPath("$.categoryIds", hasSize(1)))
                .andExpect(jsonPath("$.interestIds", hasSize(1)))
                .andExpect(jsonPath("$.kind").value("TOURISM"));
        // An empty list does empty them.
        mvc.perform(put("/api/v1/regions/" + region).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ancient Capitals\",\"code\":\"ANCIENT-1\",\"categoryIds\":[]}"))
                .andExpect(jsonPath("$.categoryIds", hasSize(0)))
                .andExpect(jsonPath("$.tagIds", hasSize(1)));
    }

    /**
     * A tourism region added from the dashboard sends no code: it is made from
     * the name, cut at a word, numbered when taken, and kept by an edit.
     */
    @Test
    void aTourismRegionHasACoverPicture() throws Exception {
        String key = "region/5d9a2d7e-0c2b-4f6a-9a0c-1f1e2d3c4b5a/angkor-at-dawn.jpg";
        String region = create("/api/v1/regions", "{\"name\":\"Covered Region\",\"code\":\"COVER-1\",\"kind\":\"TOURISM\",\"coverKey\":\"" + key + "\"}");
        mvc.perform(get("/api/v1/regions/" + region))
                .andExpect(jsonPath("$.coverKey").value(key))
                .andExpect(jsonPath("$.coverUrl").value(notNullValue()));
        // A save that says nothing about the picture keeps it; an empty key removes it; a bad key is refused.
        mvc.perform(put("/api/v1/regions/" + region).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Covered Region\",\"code\":\"COVER-1\"}"))
                .andExpect(jsonPath("$.coverKey").value(key));
        mvc.perform(put("/api/v1/regions/" + region).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Covered Region\",\"code\":\"COVER-1\",\"coverKey\":\"\"}"))
                .andExpect(jsonPath("$.coverKey").value(nullValue()))
                .andExpect(jsonPath("$.coverUrl").value(nullValue()));
        mvc.perform(put("/api/v1/regions/" + region).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Covered Region\",\"code\":\"COVER-1\",\"coverKey\":\"not a key\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aNewRegionWithoutACodeIsGivenOneFromItsName() throws Exception {
        String spa = create("/api/v1/regions", "{\"name\":\"Wellness & Spa\",\"kind\":\"TOURISM\"}");
        mvc.perform(get("/api/v1/regions/" + spa))
                .andExpect(jsonPath("$.code").value("WELLNESS-SPA"))
                .andExpect(jsonPath("$.kind").value("TOURISM"));
        String again = create("/api/v1/regions", "{\"name\":\"Wellness & Spa\",\"kind\":\"TOURISM\"}");
        mvc.perform(get("/api/v1/regions/" + again)).andExpect(jsonPath("$.code").value("WELLNESS-SPA-2"));
        String rivers = create("/api/v1/regions", "{\"name\":\"Rivers, Lakes and Floating Villages\",\"kind\":\"TOURISM\"}");
        mvc.perform(get("/api/v1/regions/" + rivers)).andExpect(jsonPath("$.code").value("RIVERS-LAKES-AND"));

        mvc.perform(put("/api/v1/regions/" + spa).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wellness & Spa Retreats\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WELLNESS-SPA"))
                .andExpect(jsonPath("$.name").value("Wellness & Spa Retreats"));
    }

    /**
     * A region's categories and filters are in its own order, as sent: moved
     * all the way round, and with one deleted from the middle, the rest keep
     * their places.
     */
    @Test
    void aRegionKeepsItsCategoriesAndFiltersInItsOwnOrder() throws Exception {
        String a = create("/api/v1/categories", "{\"name\":\"Order A\"}");
        String b = create("/api/v1/categories", "{\"name\":\"Order B\"}");
        String c = create("/api/v1/categories", "{\"name\":\"Order C\"}");
        String t1 = create("/api/v1/tags", "{\"name\":\"Order T1\"}");
        String t2 = create("/api/v1/tags", "{\"name\":\"Order T2\"}");
        String region = create("/api/v1/regions", "{\"name\":\"Order Region\",\"code\":\"ORDER-1\",\"kind\":\"TOURISM\","
                + "\"categoryIds\":" + ids(c, a, b) + ",\"tagIds\":" + ids(t2, t1) + "}");
        mvc.perform(get("/api/v1/regions/" + region))
                .andExpect(jsonPath("$.categoryIds[0]").value(c))
                .andExpect(jsonPath("$.categoryIds[1]").value(a))
                .andExpect(jsonPath("$.categoryIds[2]").value(b))
                .andExpect(jsonPath("$.tagIds[0]").value(t2))
                .andExpect(jsonPath("$.tagIds[1]").value(t1));

        mvc.perform(put("/api/v1/regions/" + region).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Order Region\",\"categoryIds\":" + ids(b, a, c) + ",\"tagIds\":" + ids(t1, t2) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryIds[0]").value(b))
                .andExpect(jsonPath("$.categoryIds[2]").value(c))
                .andExpect(jsonPath("$.tagIds[0]").value(t1));

        mvc.perform(delete("/api/v1/categories/" + a).with(owner())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/tags/" + t1).with(owner())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/regions/" + region))
                .andExpect(jsonPath("$.categoryIds", hasSize(2)))
                .andExpect(jsonPath("$.categoryIds[0]").value(b))
                .andExpect(jsonPath("$.categoryIds[1]").value(c))
                .andExpect(jsonPath("$.tagIds", hasSize(1)))
                .andExpect(jsonPath("$.tagIds[0]").value(t2));
    }

    /** The tourism regions are listed in their numbered order: a new one goes last unless given its place. */
    @Test
    void theTourismRegionsAreListedInTheirNumberedOrder() throws Exception {
        String last = create("/api/v1/regions", "{\"name\":\"Aardvark Order\",\"code\":\"ORDER-LAST\",\"kind\":\"TOURISM\"}");
        String first = create("/api/v1/regions", "{\"name\":\"Zebra Order\",\"code\":\"ORDER-FIRST\",\"kind\":\"TOURISM\",\"sortOrder\":0}");
        List<Map<String, Object>> listed = JsonPath.read(mvc.perform(get("/api/v1/regions").param("kind", "TOURISM"))
                .andReturn().getResponse().getContentAsString(), "$");
        assertThat(listed.getFirst().get("id")).isEqualTo(first);
        assertThat(listed.getLast().get("id")).isEqualTo(last);
        assertThat(listed.stream().map(row -> row.get("sortOrder")).toList())
                .as("numbered 0, 1, 2… without gaps")
                .isEqualTo(java.util.stream.IntStream.range(0, listed.size()).boxed().toList());

        // Moved to second place, it takes it; the one there moves down.
        mvc.perform(put("/api/v1/regions/" + last).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aardvark Order\",\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder").value(1));
        List<String> order = JsonPath.read(mvc.perform(get("/api/v1/regions").param("kind", "TOURISM"))
                .andReturn().getResponse().getContentAsString(), "$[*].id");
        assertThat(order.subList(0, 2)).containsExactly(first, last);
    }

    /** Interests are numbered within their group: a place given is taken, and the rest move along. */
    @Test
    void anInterestTakesItsPlaceInItsGroup() throws Exception {
        String x = create("/api/v1/interests", "{\"name\":\"Order X\",\"groupName\":\"Order Group\"}");
        String y = create("/api/v1/interests", "{\"name\":\"Order Y\",\"groupName\":\"Order Group\"}");
        String z = create("/api/v1/interests", "{\"name\":\"Order Z\",\"groupName\":\"Order Group\",\"sortOrder\":0}");
        assertThat(inGroup("Order Group")).containsExactly(z, x, y);

        mvc.perform(put("/api/v1/interests/" + y).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Order Y\",\"groupName\":\"Order Group\",\"sortOrder\":0}"))
                .andExpect(status().isOk());
        assertThat(inGroup("Order Group")).containsExactly(y, z, x);

        // Saved without a place, it stays where it is.
        mvc.perform(put("/api/v1/interests/" + x).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Order X, renamed\",\"groupName\":\"Order Group\"}"))
                .andExpect(status().isOk());
        assertThat(inGroup("Order Group")).containsExactly(y, z, x);
    }

    private List<String> inGroup(String group) throws Exception {
        return JsonPath.read(mvc.perform(get("/api/v1/interests")).andReturn().getResponse().getContentAsString(),
                "$[?(@.groupName == '" + group + "')].id");
    }

    private static String ids(String... ids) {
        return "[\"" + String.join("\",\"", ids) + "\"]";
    }

    @Test
    void aRegionCannotSitInsideItself() throws Exception {
        String parent = create("/api/v1/regions", "{\"name\":\"Cambodia\",\"code\":\"KH\"}");
        String child = create("/api/v1/regions", "{\"name\":\"Coast\",\"code\":\"COAST-2\",\"parentId\":\"" + parent + "\"}");
        mvc.perform(put("/api/v1/regions/" + parent).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cambodia\",\"code\":\"KH\",\"parentId\":\"" + child + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void termsGetSlugsFromTheirNamesAndKeepTheirOrder() throws Exception {
        String first = create("/api/v1/categories", "{\"name\":\"Night Market\",\"sortOrder\":2,\"iconKey\":\"moon\"}");
        create("/api/v1/categories", "{\"name\":\"Temples\",\"sortOrder\":1}");
        mvc.perform(get("/api/v1/categories/" + first))
                .andExpect(jsonPath("$.slug").value("night-market"))
                .andExpect(jsonPath("$.iconKey").value("moon"));
        mvc.perform(get("/api/v1/categories")).andExpect(jsonPath("$[0].name").value("Temples"));

        String tag = create("/api/v1/tags", "{\"name\":\"Fish Amok\"}");
        mvc.perform(put("/api/v1/tags/" + tag).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fish amok\",\"isActive\":false}"))
                .andExpect(jsonPath("$.slug").value("fish-amok"))
                .andExpect(jsonPath("$.isActive").value(false));
        mvc.perform(post("/api/v1/interests").with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Diving\",\"slug\":\"Not A Slug\"}"))
                .andExpect(status().isBadRequest());
    }

    /* A taxonomy list says who made each row, as every other list in the dashboard does. */
    @Test
    void aTermRemembersWhoMadeIt() throws Exception {
        RequestPostProcessor curatorWithAnAccount = staff("curator@taxonomy.test", "EDITOR", "PERM_TAXONOMY_MANAGE");
        String account = JsonPath.read(mvc.perform(get("/api/v1/users/me").with(curatorWithAnAccount))
                .andReturn().getResponse().getContentAsString(), "$.id");

        for (String list : new String[] {"/api/v1/categories", "/api/v1/tags", "/api/v1/interests"}) {
            String body = mvc.perform(post(list).with(curatorWithAnAccount).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Made on " + list.substring(1) + "\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.createdBy").value(account))
                    .andReturn().getResponse().getContentAsString();
            mvc.perform(get(list + "/" + JsonPath.<String>read(body, "$.id")))
                    .andExpect(jsonPath("$.createdBy").value(account));
        }
        /* A province is a curator's row too, and says so beside its name. */
        String zone = create("/api/v1/regions", "{\"name\":\"Author Zone\",\"code\":\"AUTHOR-ZONE\"}");
        String province = mvc.perform(post("/api/v1/provinces").with(curatorWithAnAccount).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regionId\":\"" + zone + "\",\"name\":\"Made by a curator\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value(account))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/v1/provinces/" + JsonPath.<String>read(province, "$.id")))
                .andExpect(jsonPath("$.createdBy").value(account));

        /* A seeded row, and one made by a token with no identity, simply has no author. */
        mvc.perform(get("/api/v1/categories/" + create("/api/v1/categories", "{\"name\":\"Nobody made me\"}")))
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    void corridorsAreCuratorsAndHaveNoStopsUntilTheyAreGiven() throws Exception {
        mvc.perform(post("/api/v1/corridors").with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Coastal Discovery\"}"))
                .andExpect(status().isForbidden());
        String body = mvc.perform(post("/api/v1/corridors").with(curator()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Coastal Discovery\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("coastal-discovery"))
                .andExpect(jsonPath("$.coverUrl").isEmpty())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/v1/corridor-destinations").param("corridorId", (String) JsonPath.read(body, "$.id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/v1/corridors/" + JsonPath.read(body, "$.id")).with(curator()))
                .andExpect(status().isForbidden());
    }

    @Test
    void readsArePublicAndWritesAreTheManagers() throws Exception {
        mvc.perform(get("/api/v1/regions")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/interests")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/tags").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/tags").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CONTRIBUTOR")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
        String tag = create("/api/v1/tags", "{\"name\":\"Kept\"}");
        mvc.perform(delete("/api/v1/tags/" + tag).with(manager())).andExpect(status().isForbidden());
    }

    private String create(String path, String json) throws Exception {
        String body = mvc.perform(post(path).with(manager()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static RequestPostProcessor manager() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_EDITOR"),
                new SimpleGrantedAuthority("ROLE_PERM_TAXONOMY_MANAGE"));
    }

    private static RequestPostProcessor curator() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_EDITOR"),
                new SimpleGrantedAuthority("ROLE_PERM_CORRIDOR_CURATE"));
    }

    private static RequestPostProcessor owner() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_PERM_FULL_CONTROL"));
    }
}
