package ams.asmtravel.api.services.audit;

import ams.asmtravel.api.services.audit.service.AuditService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.staff;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every catalogue write is on the record: who, from where, and what changed
 * from what. Not rolled back like the other tests: an entry is written as its
 * change is flushed, and only a committed request flushes inside the request
 * it records. What these tests write, they delete.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
class AuditLogIntegrationTests {

    private static final String ACTOR = "auditowner@desk.test";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuditService auditService;

    @AfterEach
    void forget() {
        jdbc.update("delete from taxonomy.regions where code in ('AUDIT', 'PURGE', 'OLDG')");
        jdbc.update("delete from system.audit_logs");
        for (String table : new String[] {"auth.user_roles", "auth.auth_identities", "auth.user_preferences"}) {
            jdbc.update("delete from " + table + " where user_id in (select id from auth.users where email in (?, ?))",
                    ACTOR, "auditor@desk.test");
        }
        jdbc.update("delete from auth.users where email in (?, ?)", ACTOR, "auditor@desk.test");
    }

    @Test
    void aWriteIsRecordedWithItsAuthorAndItsValues() throws Exception {
        String id = JsonPath.read(mvc.perform(post("/api/v1/regions").with(actor())
                        .header("X-Forwarded-For", "203.0.113.9, 10.0.0.2").header("User-Agent", "AuditTest/1.0")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Audit Coast\",\"code\":\"AUDIT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(put("/api/v1/regions/" + id).with(actor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Audit Shore\",\"code\":\"AUDIT\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/audit").param("entityType", "Region").param("entityId", id).with(actor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action").value("UPDATE"))
                .andExpect(jsonPath("$[0].oldData.name").value("Audit Coast"))
                .andExpect(jsonPath("$[0].newData.name").value("Audit Shore"))
                .andExpect(jsonPath("$[1].action").value("CREATE"))
                .andExpect(jsonPath("$[1].oldData").value(nullValue()))
                .andExpect(jsonPath("$[1].newData.code").value("AUDIT"))
                .andExpect(jsonPath("$[1].ipAddress").value("203.0.113.9"))
                .andExpect(jsonPath("$[1].userAgent").value("AuditTest/1.0"))
                .andExpect(jsonPath("$[1].userId").value(notNullValue()))
                .andExpect(jsonPath("$[1].userName").value("Staff auditowner"));

        mvc.perform(delete("/api/v1/regions/" + id).with(actor())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/audit").param("entityId", id).param("action", "DELETE").with(actor()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].oldData.name").value("Audit Shore"));
    }

    @Test
    void readingTheLogAndClearingItAreDifferentPrivileges() throws Exception {
        mvc.perform(post("/api/v1/regions").with(actor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Purge Bay\",\"code\":\"PURGE\"}"))
                .andExpect(status().isCreated());

        RequestPostProcessor auditor = staff("auditor@desk.test", "ADMIN", "PERM_AUDIT_VIEW");
        mvc.perform(get("/api/v1/admin/audit")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/audit").with(editor())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/audit").with(auditor)).andExpect(status().isForbidden());

        // The answer is the entry the clearing leaves behind, which says how many went.
        int removed = JsonPath.read(mvc.perform(delete("/api/v1/admin/audit").with(actor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("PURGE"))
                .andReturn().getResponse().getContentAsString(), "$.newData.removed");
        assertThat(removed).isPositive();
        mvc.perform(get("/api/v1/admin/audit").with(auditor))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("PURGE"))
                .andExpect(jsonPath("$[0].userName").value("Staff auditowner"))
                .andExpect(jsonPath("$[0].newData.removed").value(removed));
    }

    @Test
    void entriesOlderThanTheCutoffAreTrimmed() throws Exception {
        mvc.perform(post("/api/v1/regions").with(actor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Old Gulf\",\"code\":\"OLDG\"}"))
                .andExpect(status().isCreated());
        assertThat(auditService.prune(Instant.now().minus(1, ChronoUnit.DAYS))).isZero();
        assertThat(auditService.prune(Instant.now().plus(1, ChronoUnit.MINUTES))).isPositive();
        assertThat(auditService.retentionDays()).isEqualTo(365);
    }

    private static RequestPostProcessor actor() {
        return staff(ACTOR, "ADMIN", "PERM_FULL_CONTROL", "PERM_AUDIT_VIEW");
    }
}
