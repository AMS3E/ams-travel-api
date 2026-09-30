package ams.asmtravel.api.services.admin;

import ams.asmtravel.api.services.destination.destination.domain.DestinationThemes;
import ams.asmtravel.api.services.destination.stay.domain.StayAmenities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static ams.asmtravel.api.support.Callers.editor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What a caller is told when a body cannot be read, and what the document
 * tells them beforehand.
 *
 * <p>Every field a request takes should be answerable from the published
 * document and, when it is wrong, from the refusal. Both were once silent: a
 * value of the wrong sort answered "Failed to read request" without naming the
 * field, and the lists a listing chooses its words from were enforced but
 * never published, so the only way to learn them was to be refused.</p>
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class RequestContractTests {

    @Autowired MockMvc mvc;

    @Test
    void aValueOfTheWrongSortNamesItsFieldAndWhatItTakes() throws Exception {
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside\",\"checkInFrom\":\"soon\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Check in from")))
                .andExpect(jsonPath("$.detail").value(containsString("14:00")))
                .andExpect(jsonPath("$.errors[0].field").value("checkInFrom"));
    }

    @Test
    void aWordOutsideAFixedListIsAnsweredWithTheList() throws Exception {
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside\",\"type\":\"CASTLE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Type")))
                .andExpect(jsonPath("$.detail").value(containsString("HOTEL")));
    }

    @Test
    void aSingleWordWhereAListBelongsSaysSo() throws Exception {
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside\",\"amenities\":\"wifi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Amenities")))
                .andExpect(jsonPath("$.detail").value(containsString("takes a list")));
    }

    @Test
    void abodyThatIsNotJsonSaysThatMuchAndNoMore() throws Exception {
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("not valid JSON")));
    }

    /**
     * The words a listing chooses from, in the document rather than only in the
     * refusal: a stay's amenities, a place's theme.
     */
    @Test
    void theFixedListsArePublishedWithTheSchemasThatUseThem() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.StayRequest.properties.amenities.items.enum")
                        .value(hasSize(StayAmenities.ALL.size())))
                .andExpect(jsonPath("$.components.schemas.StayRequest.properties.amenities.items.enum")
                        .value(hasItem("wifi")))
                .andExpect(jsonPath("$.components.schemas.DestinationRequest.properties.theme.enum")
                        .value(hasSize(DestinationThemes.BY_THEME.size())))
                .andExpect(jsonPath("$.components.schemas.FoodRequest.properties.cuisines.items.enum")
                        .value(hasItem("khmer")))
                /* A time says what a time looks like, which "string" never did. */
                .andExpect(jsonPath("$.components.schemas.StayRequest.properties.checkInFrom.format").value("partial-time"))
                .andExpect(jsonPath("$.components.schemas.StayRequest.properties.checkInFrom.example").value("14:00"));
    }
}
