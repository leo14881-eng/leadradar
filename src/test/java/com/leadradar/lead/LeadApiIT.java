package com.leadradar.lead;

import com.leadradar.merchant.Merchant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeadApiIT {
    @Autowired
    MockMvc mvc;
    @Autowired
    LeadRepository repo;

    private final Merchant m = new Merchant("api-m", "店", "中餐厅", List.of(), List.of(), List.of(), null, "");

    @Test
    void markSeenIsIdempotent() {
        assertTrue(repo.markSeen("k:1"));
        assertFalse(repo.markSeen("k:1"));
    }

    @Test
    void patchStatusRecordsOutcome() throws Exception {
        long id = repo.save(new Lead(new Post("fb", "9", "https://x/9", "a", "text", Instant.now()), m, "s", "r", 80));

        mvc.perform(patch("/api/leads/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WON\",\"note\":\"4人火锅\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.note").value("4人火锅"));

        mvc.perform(get("/api/leads").param("merchantId", "api-m").param("status", "WON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void patchUnknownLeadIs404AndMissingStatusIs400() throws Exception {
        mvc.perform(patch("/api/leads/999999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WON\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/leads/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualImportEndpointQueuesPost() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\",\"url\":\"https://x/q\"}"))
                .andExpect(status().isOk());
    }
}
