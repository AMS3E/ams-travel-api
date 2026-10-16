package ams.asmtravel.api.services.destination.destination;

import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.repository.UserRepository;
import ams.asmtravel.api.services.corridor.repository.CorridorStopRepository;
import ams.asmtravel.api.services.media.repository.MediaRepository;
import ams.asmtravel.api.services.destination.food.repository.FoodRepository;
import ams.asmtravel.api.services.destination.stay.repository.StayRepository;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.taxonomy.category.repository.CategoryRepository;
import ams.asmtravel.api.services.corridor.repository.CorridorRepository;
import ams.asmtravel.api.services.location.province.repository.ProvinceRepository;
import ams.asmtravel.api.services.location.region.repository.RegionRepository;
import com.jayway.jsonpath.JsonPath;
import io.minio.MinioClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

import static ams.asmtravel.api.support.Callers.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class DestinationIntegrationTests {

    private static final String COVER = "destination/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";

    @Autowired MockMvc mvc;
    @MockitoBean MinioClient minioClient;
    @Autowired EntityManager entityManager;
    @Autowired RegionRepository regions;
    @Autowired ProvinceRepository provinces;
    @Autowired CategoryRepository categories;
    @Autowired CorridorRepository corridors;
    @Autowired UserRepository users;
    @Autowired StayRepository stays;
    @Autowired FoodRepository foods;
    @Autowired MediaRepository media;
    @Autowired CorridorStopRepository stops;

    private Province province;
    private Category temples;

    @BeforeEach
    void catalogue() {
        Region region = new Region();
        region.setName("Angkor & the Tonle Sap");
        region.setCode("ANGKOR-T");
        region = regions.save(region);
        province = new Province();
        province.setRegion(region);
        province.setName("Siem Reap");
        province.setSlug("siem-reap-test");
        province = provinces.save(province);
        temples = new Category();
        temples.setName("Temples");
        temples.setSlug("temples-t");
        temples = categories.save(temples);
    }

    @Test
    void aContributorsDraftIsTheirsUntilItIsPublished() throws Exception {
        RequestPostProcessor contributor = staff("writer@desk.test", "CONTRIBUTOR",
                "PERM_DESTINATION_DRAFT_CREATE", "PERM_DESTINATION_DRAFT_EDIT_OWN");
        String id = create(contributor, "{\"name\":\"Bayon Temple\",\"provinceId\":\"" + province.getId()
                + "\",\"coverKey\":\"" + COVER + "\",\"categoryIds\":[\"" + temples.getId() + "\"]}");

        mvc.perform(get("/api/v1/destinations/" + id).with(contributor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("bayon-temple"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.regionId").value(province.getRegion().getId().toString()))
                .andExpect(jsonPath("$.coverUrl").value("http://localhost:8080/api/v1/media/files/" + COVER))
                .andExpect(jsonPath("$.categoryIds[0]").value(temples.getId().toString()));
        mvc.perform(get("/api/v1/destinations/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/destinations")).andExpect(jsonPath("$[*].id", not(hasItem(id))));

        mvc.perform(patch("/api/v1/destinations/" + id + "/publish").with(contributor)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/destinations/" + id + "/publish").with(owner()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        mvc.perform(get("/api/v1/destinations").param("slug", "bayon-temple"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(put("/api/v1/destinations/" + id).with(contributor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bayon\",\"provinceId\":\"" + province.getId() + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/destinations/" + id).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bayon\",\"provinceId\":\"" + province.getId() + "\"}"))
                .andExpect(jsonPath("$.name").value("Bayon"))
                .andExpect(jsonPath("$.slug").value("bayon-temple"))
                .andExpect(jsonPath("$.categoryIds[0]").value(temples.getId().toString()));

        mvc.perform(patch("/api/v1/destinations/" + id + "/unpublish").with(editor()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    /**
     * A place is shelved in the tourism regions chosen for it, not in every
     * region one of its categories happens to belong to. Its zone is where its
     * province is, and cannot be chosen.
     */
    @Test
    void aPlaceIsShelvedInTheTourismRegionsChosenForIt() throws Exception {
        Region capitals = theme("Ancient Capitals & Khmer Civilization", "CAPITALS-T");
        Region culinary = theme("Khmer Culinary", "CULINARY-T");
        String id = create(owner(), "{\"name\":\"Phnom Da\",\"provinceId\":\"" + province.getId()
                + "\",\"categoryIds\":[\"" + temples.getId()
                + "\"],\"tourismRegionIds\":[\"" + capitals.getId() + "\"]}");

        mvc.perform(get("/api/v1/destinations/" + id).with(editor()))
                .andExpect(jsonPath("$.tourismRegionIds", contains(capitals.getId().toString())))
                .andExpect(jsonPath("$.regionId").value(province.getRegion().getId().toString()));
        mvc.perform(post("/api/v1/destinations").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wat Phnom\",\"provinceId\":\"" + province.getId()
                                + "\",\"tourismRegionIds\":[\"" + province.getRegion().getId() + "\"]}"))
                .andExpect(status().isBadRequest());

        // Published and browsed once, it counts on its tourism region's page as well as its zone's.
        mvc.perform(patch("/api/v1/destinations/" + id + "/publish").with(owner())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/popular/events").with(customer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + id + "\",\"eventType\":\"DESTINATION_VIEW\"}"))
                .andExpect(status().isAccepted());
        nextRequest();
        mvc.perform(get("/api/v1/popular").param("regionId", capitals.getId().toString()))
                .andExpect(jsonPath("$.destinations[*].destinationId", hasItem(id)));
        mvc.perform(get("/api/v1/popular").param("regionId", culinary.getId().toString()))
                .andExpect(jsonPath("$.destinations[*].destinationId", not(hasItem(id))));

        // Left out of an edit, the choice stands; sent, it is replaced.
        mvc.perform(put("/api/v1/destinations/" + id).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phnom Da\",\"provinceId\":\"" + province.getId() + "\"}"))
                .andExpect(jsonPath("$.tourismRegionIds", contains(capitals.getId().toString())));
        mvc.perform(put("/api/v1/destinations/" + id).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phnom Da\",\"provinceId\":\"" + province.getId()
                                + "\",\"tourismRegionIds\":[\"" + culinary.getId() + "\"]}"))
                .andExpect(jsonPath("$.tourismRegionIds", contains(culinary.getId().toString())));

        nextRequest();
        mvc.perform(delete("/api/v1/regions/" + culinary.getId()).with(owner())).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/destinations/" + id).with(editor())).andExpect(jsonPath("$.tourismRegionIds", hasSize(0)));
    }

    /** A tourism region made in the dashboard is a theme too: it has no specialties of its own. */
    @Test
    void anAttractionCanBeListedInANewTourismRegion() throws Exception {
        Region silk = new Region();
        silk.setName("Silk Road Weaving");
        silk.setCode("SILK-ROAD");
        silk.setKind(RegionKind.TOURISM);
        silk = regions.save(silk);
        String id = create(owner(), "{\"name\":\"Koh Dach Silk Island\",\"provinceId\":\"" + province.getId()
                + "\",\"theme\":\"SILK-ROAD\",\"tourismRegionIds\":[\"" + silk.getId() + "\"]}");
        mvc.perform(get("/api/v1/destinations/" + id).with(editor()))
                .andExpect(jsonPath("$.theme").value("SILK-ROAD"))
                .andExpect(jsonPath("$.tourismRegionIds", contains(silk.getId().toString())));
        // A zone the provinces sit in is not a theme.
        Region zone = new Region();
        zone.setName("Far North Zone");
        zone.setCode("FAR-NORTH");
        zone.setKind(RegionKind.ZONE);
        regions.save(zone);
        mvc.perform(post("/api/v1/destinations").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Zoned Temple\",\"provinceId\":\"" + province.getId() + "\",\"theme\":\"FAR-NORTH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such theme: FAR-NORTH"));
    }

    /** An attraction's theme and what only it has: a beach's white sand and calm water. Left out of an edit, both stand. */
    @Test
    void anAttractionKeepsItsThemeAndWhatOnlyItHas() throws Exception {
        String id = create(owner(), "{\"name\":\"Long Set Beach\",\"provinceId\":\"" + province.getId()
                + "\",\"theme\":\"COASTAL-ISLAND\",\"specialties\":[\"white-sand\",\"calm-water\",\"by-speedboat\"]}");
        mvc.perform(get("/api/v1/destinations/" + id).with(editor()))
                .andExpect(jsonPath("$.theme").value("COASTAL-ISLAND"))
                .andExpect(jsonPath("$.specialties", contains("white-sand", "calm-water", "by-speedboat")));
        mvc.perform(put("/api/v1/destinations/" + id).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Long Set Beach\",\"provinceId\":\"" + province.getId() + "\"}"))
                .andExpect(jsonPath("$.theme").value("COASTAL-ISLAND"))
                .andExpect(jsonPath("$.specialties", hasSize(3)));
        // Ancient Capitals is a theme with nothing of its own: its tags tell a site's history.
        create(owner(), "{\"name\":\"Ta Prohm\",\"provinceId\":\"" + province.getId() + "\",\"theme\":\"ANCIENT-CAPITALS\"}");
        mvc.perform(post("/api/v1/destinations").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bayon\",\"provinceId\":\"" + province.getId()
                                + "\",\"theme\":\"ANCIENT-CAPITALS\",\"specialties\":[\"angkor-pass\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such specialty: angkor-pass"));
        mvc.perform(post("/api/v1/destinations").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Moon Temple\",\"provinceId\":\"" + province.getId() + "\",\"theme\":\"MOON\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such theme: MOON"));
        mvc.perform(post("/api/v1/destinations").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Odd Temple\",\"provinceId\":\"" + province.getId() + "\",\"specialties\":[\"lava-slide\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such specialty: lava-slide"));
    }

    /** A form that sends something the API cannot take is told which field, and what to change: never just "Invalid request content." */
    @Test
    void aRefusedDestinationSaysWhichFieldsToFix() throws Exception {
        mvc.perform(post("/api/v1/destinations").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wat Phnom\",\"slug\":\"Wat Phnom\",\"provinceId\":\"" + province.getId()
                                + "\",\"coverKey\":\"https://example.com/wat-phnom.jpg\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Slug: Use lower-case words joined by hyphens")))
                .andExpect(jsonPath("$.detail", containsString("Cover key: Upload the cover first, then send its key")))
                .andExpect(jsonPath("$.detail", not(containsString("Invalid request content"))))
                .andExpect(jsonPath("$.errors[0].field").exists());
        nextRequest();
        mvc.perform(post("/api/v1/destinations").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Name: is required.")))
                .andExpect(jsonPath("$.detail", containsString("Province: is required.")));
    }

    /** A BDO's desk is what partners write; the partner is known by the role their last sign-in mirrored. */
    @Test
    void aBdoReachesPartnerDraftsButNotContributorOnes() throws Exception {
        RequestPostProcessor partner = staff("partner@desk.test", "PARTNER", "PERM_LISTING_EDIT_OWN");
        RequestPostProcessor contributor = staff("other@desk.test", "CONTRIBUTOR", "PERM_DESTINATION_DRAFT_CREATE",
                "PERM_DESTINATION_DRAFT_EDIT_OWN");
        RequestPostProcessor bdo = staff("bdo@desk.test", "BDO", "PERM_PARTNER_PUBLISH_APPROVE", "PERM_PARTNER_LISTING_EDIT");
        String partnerDraft = create(partner, "{\"name\":\"Riverside Lodge\",\"provinceId\":\"" + province.getId() + "\"}");
        String contributorDraft = create(contributor, "{\"name\":\"Old Market\",\"provinceId\":\"" + province.getId() + "\"}");

        mvc.perform(get("/api/v1/destinations/" + partnerDraft).with(bdo)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/destinations/" + partnerDraft).with(bdo).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside Lodge Kampot\",\"provinceId\":\"" + province.getId() + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/destinations/" + contributorDraft).with(bdo)).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/destinations/" + contributorDraft).with(bdo).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nope\",\"provinceId\":\"" + province.getId() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aGalleryCoverIsTheDestinationsCoverAndDraftGalleriesArePrivate() throws Exception {
        String id = create(editor(), "{\"name\":\"Ta Prohm\",\"provinceId\":\"" + province.getId() + "\"}");
        String first = createMedia(id, "{\"ownerType\":\"DESTINATION\",\"ownerId\":\"" + id + "\",\"mediaKey\":\"" + COVER + "\",\"isCover\":true}");
        String second = createMedia(id, "{\"ownerType\":\"DESTINATION\",\"ownerId\":\"" + id + "\",\"mediaKey\":\"" + COVER.replace(".jpg", ".png")
                + "\",\"isCover\":true,\"sortOrder\":2}");

        mvc.perform(get("/api/v1/media").param("ownerType", "DESTINATION").param("ownerId", id).with(editor()))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id=='" + first + "')].isCover").value(contains(false)))
                .andExpect(jsonPath("$[?(@.id=='" + second + "')].isCover").value(contains(true)));
        mvc.perform(get("/api/v1/destinations/" + id).with(editor()))
                .andExpect(jsonPath("$.coverKey").value(COVER.replace(".jpg", ".png")));
        mvc.perform(get("/api/v1/media").param("ownerType", "DESTINATION").param("ownerId", id)).andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(post("/api/v1/media").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerType\":\"DESTINATION\",\"ownerId\":\"" + id + "\",\"mediaKey\":\"https://elsewhere.test/a.jpg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUploadJoinsTheGalleryAndAHiddenPhotoLeavesThePublicSite() throws Exception {
        String id = create(editor(), "{\"name\":\"Banteay Srei\",\"provinceId\":\"" + province.getId() + "\"}");
        MockMultipartFile png = new MockMultipartFile("files", "srei.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0});
        String body = mvc.perform(multipart("/api/v1/media").file(png).with(editor())
                        .param("ownerType", "DESTINATION").param("ownerId", id).param("isCover", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].mediaKey", startsWith("destination/")))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String uploaded = JsonPath.read(body, "$[0].id");
        createMedia(id, "{\"ownerType\":\"DESTINATION\",\"ownerId\":\"" + id + "\",\"mediaKey\":\"" + COVER + "\",\"sortOrder\":2}");
        mvc.perform(get("/api/v1/destinations/" + id).with(editor()))
                .andExpect(jsonPath("$.coverKey").value((String) JsonPath.read(body, "$[0].mediaKey")));
        mvc.perform(patch("/api/v1/destinations/" + id + "/publish").with(owner())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/media").param("ownerType", "DESTINATION").param("ownerId", id)).andExpect(jsonPath("$", hasSize(2)));

        // Hiding the cover takes it off the public site and off the destination's cover.
        mvc.perform(put("/api/v1/media/" + uploaded).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.isCover").value(false));
        mvc.perform(get("/api/v1/media").param("ownerType", "DESTINATION").param("ownerId", id)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/media").param("ownerType", "DESTINATION").param("ownerId", id).with(editor()))
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/v1/media/" + uploaded)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/destinations/" + id).with(editor())).andExpect(jsonPath("$.coverKey").value(nullValue()));

        mvc.perform(put("/api/v1/media/" + uploaded).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isCover\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void severalPhotosUploadInOneCallAndJoinTheGalleryInOrder() throws Exception {
        String id = create(editor(), "{\"name\":\"Preah Khan\",\"provinceId\":\"" + province.getId() + "\"}");
        createMedia(id, "{\"ownerType\":\"DESTINATION\",\"ownerId\":\"" + id + "\",\"mediaKey\":\"" + COVER + "\",\"sortOrder\":4}");
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0};
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

        mvc.perform(multipart("/api/v1/media").with(editor())
                        .file(new MockMultipartFile("files", "a.png", "image/png", png))
                        .file(new MockMultipartFile("files", "b.jpg", "image/jpeg", jpg))
                        .file(new MockMultipartFile("files", "c.png", "image/png", png))
                        .param("ownerType", "DESTINATION").param("ownerId", id).param("isCover", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].sortOrder", contains(5, 6, 7)))
                .andExpect(jsonPath("$[*].isCover", contains(true, false, false)))
                .andExpect(jsonPath("$[1].mediaKey", endsWith(".jpg")));
        mvc.perform(get("/api/v1/media").param("ownerType", "DESTINATION").param("ownerId", id).with(editor()))
                .andExpect(jsonPath("$", hasSize(4)));

        // One bad file stops the whole batch before anything is stored.
        mvc.perform(multipart("/api/v1/media").with(editor())
                        .file(new MockMultipartFile("files", "ok.png", "image/png", png))
                        .file(new MockMultipartFile("files", "page.html", "image/png", "<html>".getBytes()))
                        .param("ownerType", "DESTINATION").param("ownerId", id))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/media").param("ownerType", "DESTINATION").param("ownerId", id).with(editor()))
                .andExpect(jsonPath("$", hasSize(4)));
    }

/**
     * A partner lists what is in their own name: they need not say whose it is,
     * cannot say someone else's, and cannot put one live. The public sees only
     * the live ones.
     */
    @Test
    void stayAndFoodListingsFollowTheAccountThatRunsThem() throws Exception {
        String destination = create(editor(), "{\"name\":\"Kampot River\",\"provinceId\":\"" + province.getId() + "\"}");
        User member = person("staff@partner.test");
        User somebodyElse = person("other@partner.test");
        RequestPostProcessor listing = staff("staff@partner.test", "PARTNER", "PERM_LISTING_EDIT_OWN", "PERM_LISTING_DELETE_OWN");

        String stay = JsonPath.read(mvc.perform(post("/api/v1/stays").with(listing).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + destination
                                + "\",\"name\":\"River Hut\",\"type\":\"GUESTHOUSE\",\"starRating\":3.5," + KAMPOT_PIN + ","
                                + "\"priceFrom\":25,\"amenities\":[\"wifi\",\"breakfast\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.ownerUserId").value(member.getId().toString()))
                .andExpect(jsonPath("$.amenities", contains("wifi", "breakfast")))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/v1/stays").with(listing).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + destination + "\",\"ownerUserId\":\"" + somebodyElse.getId()
                                + "\",\"name\":\"Not Mine\",\"type\":\"HOTEL\"," + KAMPOT_PIN + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/stays/" + stay).with(listing).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + destination
                                + "\",\"name\":\"River Hut\",\"type\":\"GUESTHOUSE\",\"status\":\"PUBLISHED\"," + KAMPOT_PIN + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/stays").param("destinationId", destination)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/stays").param("destinationId", destination).with(listing)).andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(put("/api/v1/stays/" + stay).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + destination + "\",\"ownerUserId\":\"" + member.getId()
                                + "\",\"name\":\"River Hut\",\"type\":\"GUESTHOUSE\",\"status\":\"PUBLISHED\"," + KAMPOT_PIN + "}"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/stays").param("destinationId", destination)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(delete("/api/v1/stays/" + stay).with(listing)).andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/foods").with(listing).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + destination
                                + "\",\"name\":\"Crab Shack\",\"type\":\"RESTAURANT\",\"priceRange\":\"$$\"," + KAMPOT_PIN + ","
                                + "\"openingHours\":{\"mon\":\"07:00-22:00\",\"sun\":\"closed\"},"
                                + "\"categoryIds\":[\"" + temples.getId() + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("crab-shack"))
                .andExpect(jsonPath("$.openingHours.mon").value("07:00-22:00"))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.averageRating").isEmpty());
        mvc.perform(post("/api/v1/foods").with(listing).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + destination
                                + "\",\"name\":\"Late Bar\",\"type\":\"BAR\"," + KAMPOT_PIN + ",\"openingHours\":{\"fri\":\"8pm till late\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aCorridorRouteIsReplacedWholeAndComesBackInOrder() throws Exception {
        String first = create(editor(), "{\"name\":\"Angkor Wat\",\"provinceId\":\"" + province.getId() + "\"}");
        String second = create(editor(), "{\"name\":\"Koh Ker\",\"provinceId\":\"" + province.getId() + "\"}");
        Corridor corridor = new Corridor();
        corridor.setName("Khmer Civilization");
        corridor.setSlug("khmer-civilization-t");
        corridor = corridors.save(corridor);
        RequestPostProcessor curator = staff("curator@desk.test", "EDITOR", "PERM_CORRIDOR_CURATE");

        mvc.perform(put("/api/v1/corridor-destinations/" + corridor.getId()).with(curator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationIds\":[\"" + second + "\",\"" + first + "\"]}"))
                .andExpect(jsonPath("$[0].destinationId").value(second))
                .andExpect(jsonPath("$[1].sortOrder").value(2));
        // A draft corridor's route is its curators' to read.
        mvc.perform(get("/api/v1/corridor-destinations").param("corridorId", corridor.getId().toString()).with(curator))
                .andExpect(jsonPath("$[*].destinationId", contains(second, first)));
        mvc.perform(put("/api/v1/corridor-destinations/" + corridor.getId()).with(curator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationIds\":[\"" + first + "\",\"" + first + "\"]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/corridor-destinations/" + corridor.getId()).with(editor())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinationIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Deleting a place takes its gallery and route stops, and leaves the stays
     * and food listings that named it: each stands on its own pin. Its
     * province keeps it from going first.
     */
    @Test
    void deletingADestinationTakesWhatHangsOffIt() throws Exception {
        String id = create(editor(), "{\"name\":\"Phnom Kulen\",\"provinceId\":\"" + province.getId() + "\"}");
        createMedia(id, "{\"ownerType\":\"DESTINATION\",\"ownerId\":\"" + id + "\",\"mediaKey\":\"" + COVER + "\"}");
        mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + id + "\",\"name\":\"Waterfall Stall\",\"type\":\"STREET_FOOD\"," + KULEN_PIN + ","
                                + "\"categoryIds\":[\"" + temples.getId() + "\"]}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + id + "\",\"name\":\"Kulen Camp\",\"type\":\"OTHER\"," + KULEN_PIN + "}"))
                .andExpect(status().isCreated());

        nextRequest();
        mvc.perform(delete("/api/v1/provinces/" + province.getId()).with(owner())).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/categories/" + temples.getId()).with(owner())).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/destinations/" + id).with(editor())).andExpect(jsonPath("$.categoryIds", hasSize(0)));
        mvc.perform(delete("/api/v1/destinations/" + id).with(owner())).andExpect(status().isNoContent());

        assertThat(foods.findByDestinationIdOrderByNameAsc(UUID.fromString(id))).isEmpty();
        assertThat(stays.findByDestinationIdOrderByNameAsc(UUID.fromString(id))).isEmpty();
        nextRequest();
        assertThat(stays.findAll()).singleElement().satisfies(stay -> {
            assertThat(stay.getName()).isEqualTo("Kulen Camp");
            assertThat(stay.getDestination()).isNull();
        });
        assertThat(foods.findAll()).singleElement().satisfies(food -> {
            assertThat(food.getName()).isEqualTo("Waterfall Stall");
            assertThat(food.getDestination()).isNull();
        });
        assertThat(media.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(ams.asmtravel.api.services.media.domain.MediaType.DESTINATION, UUID.fromString(id))).isEmpty();
        mvc.perform(delete("/api/v1/provinces/" + province.getId()).with(owner())).andExpect(status().isNoContent());
    }

    /**
     * A hotel or a restaurant is a place of its own: it names no destination,
     * only its map pin, and a destination's page finds the published ones near
     * it, nearest first.
     */
    @Test
    void staysAndFoodsAreFoundByTheirPinNearADestination() throws Exception {
        String angkor = create(editor(), "{\"name\":\"Angkor Wat\",\"provinceId\":\"" + province.getId()
                + "\",\"latitude\":13.4125,\"longitude\":103.8670}");
        String unpinned = create(editor(), "{\"name\":\"Somewhere\",\"provinceId\":\"" + province.getId() + "\"}");

        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Pin Inn\",\"type\":\"HOTEL\"}"))
                .andExpect(status().isBadRequest());

        // Six kilometres, thirty, and three hundred away; and one not live yet.
        stay("Siem Reap Inn", "13.3633", "103.8564", "PUBLISHED")
                .andExpect(jsonPath("$.destinationId").doesNotExist());
        stay("Kulen Lodge", "13.5500", "104.1000", "PUBLISHED");
        stay("Kampot Hut", "10.6104", "104.1815", "PUBLISHED");
        stay("Draft Villa", "13.3700", "103.8600", "DRAFT");
        nextRequest();

        mvc.perform(get("/api/v1/stays").param("near", angkor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Siem Reap Inn", "Kulen Lodge")));
        mvc.perform(get("/api/v1/stays").param("near", unpinned)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/stays").param("near", UUID.randomUUID().toString())).andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Pin Noodles\",\"type\":\"STREET_FOOD\"}"))
                .andExpect(status().isBadRequest());
        food("Pub Street Kitchen", "13.3547", "103.8551", "PUBLISHED")
                .andExpect(jsonPath("$.destinationId").doesNotExist());
        food("Temple Cafe", "13.4100", "103.8650", "PUBLISHED");
        food("Kampot Crab Market", "10.4955", "104.3179", "PUBLISHED");
        food("Draft Diner", "13.4000", "103.8600", "DRAFT");
        nextRequest();

        mvc.perform(get("/api/v1/foods").param("near", angkor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Temple Cafe", "Pub Street Kitchen")));
        mvc.perform(get("/api/v1/foods").param("near", unpinned)).andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * A stay is shelved under interests, as a food listing is: Explore's Stays
     * shelf lists it on each one's page. A deleted interest lets go of it.
     */
    @Test
    void aStayIsShelvedUnderItsInterests() throws Exception {
        String resorts = JsonPath.read(mvc.perform(post("/api/v1/interests").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Resorts\",\"groupName\":\"Stays\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        String stay = JsonPath.read(mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside Resort\",\"type\":\"RESORT\",\"status\":\"PUBLISHED\"," + KAMPOT_PIN
                                + ",\"interestIds\":[\"" + resorts + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interestIds", contains(resorts)))
                .andReturn().getResponse().getContentAsString(), "$.id");

        // An edit without the field keeps them.
        mvc.perform(put("/api/v1/stays/" + stay).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside Resort\",\"type\":\"RESORT\"," + KAMPOT_PIN + "}"))
                .andExpect(jsonPath("$.interestIds", contains(resorts)));
        mvc.perform(get("/api/v1/stays")).andExpect(jsonPath("$[0].interestIds", contains(resorts)));

        nextRequest();
        mvc.perform(delete("/api/v1/interests/" + resorts).with(owner())).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/stays/" + stay)).andExpect(jsonPath("$.interestIds", hasSize(0)));
    }

    private ResultActions food(String name, String latitude, String longitude, String status) throws Exception {
        return mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"RESTAURANT\",\"status\":\"" + status
                                + "\",\"latitude\":" + latitude + ",\"longitude\":" + longitude + "}"))
                .andExpect(status().isCreated());
    }

    private ResultActions stay(String name, String latitude, String longitude, String status) throws Exception {
        return mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"HOTEL\",\"status\":\"" + status
                                + "\",\"latitude\":" + latitude + ",\"longitude\":" + longitude + "}"))
                .andExpect(status().isCreated());
    }

    /* ---------------------------------------------------------------------- */

    /**
     * What a new request would see: the whole test shares one transaction, and
     * a bulk delete does not touch rows this session already holds.
     */
    private static final String KAMPOT_PIN = "\"latitude\":10.6104,\"longitude\":104.1815";
    private static final String KULEN_PIN = "\"latitude\":13.5500,\"longitude\":104.1000";

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }

    private String create(RequestPostProcessor caller, String json) throws Exception {
        String body = mvc.perform(post("/api/v1/destinations").with(caller).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String createMedia(String destinationId, String json) throws Exception {
        String body = mvc.perform(post("/api/v1/media").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private Region theme(String name, String code) {
        Region region = new Region();
        region.setName(name);
        region.setCode(code);
        region.setKind(RegionKind.TOURISM);
        return regions.save(region);
    }

    private User person(String email) {
        User user = new User();
        user.setFullName("Listing Person");
        user.setEmail(email);
        return users.save(user);
    }

    private static RequestPostProcessor staff(String email, String... grants) {
        return jwt().jwt(token -> token.subject("subject-" + email).claim("email", email))
                .authorities(Arrays.stream(grants)
                        .map(grant -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + grant))
                        .toList());
    }

    private static RequestPostProcessor editor() {
        return staff("editor@desk.test", "EDITOR", "PERM_DESTINATION_EDIT_ANY", "PERM_DESTINATION_UNPUBLISH",
                "PERM_DESTINATION_DRAFT_CREATE");
    }

    private static RequestPostProcessor owner() {
        return staff("owner@desk.test", "ADMIN", "PERM_FULL_CONTROL");
    }
}
