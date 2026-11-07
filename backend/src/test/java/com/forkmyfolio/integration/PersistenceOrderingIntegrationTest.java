package com.forkmyfolio.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Persistence + ordering: content created through the real HTTP API must be
 * persisted (survive a fresh request, i.e. a new persistence context) and
 * respect display-order semantics on both the owner and public surfaces.
 */
@Transactional
public class PersistenceOrderingIntegrationTest extends AbstractMySqlIntegrationTest {

        private static final String PASSWORD = "Password123!";

        @org.springframework.beans.factory.annotation.Autowired
        private com.forkmyfolio.service.impl.VisitorStatsService visitorStatsService;

        @org.springframework.beans.factory.annotation.Autowired
        private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

        private String email;
        private String slug;
        private String jwt;

        @BeforeEach
        void seedUserAndLogin() throws Exception {
                String suffix = runSuffix();
                email = "ordering-" + suffix + "@example.com";
                slug = "ordering-" + suffix;
                seedUser(email, slug, true);
                jwt = realLogin(email, PASSWORD);
        }

        private void createProject(String title, int displayOrder, boolean visible) throws Exception {
                mockMvc.perform(post("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "%s", "description": "Ordering test project %s",
                                                 "visible": %s, "displayOrder": %d, "skills": []}
                                                """.formatted(title, title, visible, displayOrder)))
                                .andExpect(status().isCreated());
        }

        @Test
        void creationOrder_independentOfDisplayOrder_sortedOnRead() throws Exception {
                // Create deliberately out of order
                createProject("Order-Two", 2, true);
                createProject("Order-Zero", 0, true);
                createProject("Order-One", 1, true);

                // Owner surface sorted ascending by displayOrder
                mockMvc.perform(get("/api/v1/me/projects").header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                                .jsonPath("$.data[0].title").value("Order-Zero"))
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                                .jsonPath("$.data[1].title").value("Order-One"))
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                                .jsonPath("$.data[2].title").value("Order-Two"));

                // Public surface applies the same ordering
                MvcResult publicList = mockMvc
                                .perform(get("/api/v1/portfolios/{slug}/projects", slug))
                                .andExpect(status().isOk())
                                .andReturn();
                List<String> publicTitles = new ArrayList<>();
                for (var node : objectMapper.readTree(publicList.getResponse().getContentAsString())
                                .path("data")) {
                        publicTitles.add(node.path("title").asText());
                }
                assertThat(publicTitles).containsExactly("Order-Zero", "Order-One", "Order-Two");
        }

        @Test
        void reorder_viaUpdate_persistsAcrossRequests() throws Exception {
                createProject("Persist-A", 0, true);
                createProject("Persist-B", 1, true);
                createProject("Persist-C", 2, true);

                // Move "Persist-C" to the front (0) and "Persist-A" to the end (2)
                MvcResult list = mockMvc.perform(get("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn();
                String uuidC = null;
                String uuidA = null;
                for (var node : objectMapper.readTree(list.getResponse().getContentAsString()).path("data")) {
                        if ("Persist-C".equals(node.path("title").asText())) {
                                uuidC = node.path("uuid").asText();
                        }
                        if ("Persist-A".equals(node.path("title").asText())) {
                                uuidA = node.path("uuid").asText();
                        }
                }
                assertThat(uuidA).isNotNull();
                assertThat(uuidC).isNotNull();

                mockMvc.perform(put("/api/v1/me/projects/{uuid}", uuidC)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "Persist-C", "description": "Ordering test project Persist-C",
                                                 "visible": true, "displayOrder": 0, "skills": []}
                                                """))
                                .andExpect(status().isOk());

                mockMvc.perform(put("/api/v1/me/projects/{uuid}", uuidA)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "Persist-A", "description": "Ordering test project Persist-A",
                                                 "visible": true, "displayOrder": 9, "skills": []}
                                                """))
                                .andExpect(status().isOk());

                // A BRAND-NEW GET (fresh persistence context) must reflect persisted order
                MvcResult afterReorder = mockMvc.perform(get("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn();

                List<String> titles = new ArrayList<>();
                for (var node : objectMapper.readTree(afterReorder.getResponse().getContentAsString())
                                .path("data")) {
                        titles.add(node.path("title").asText());
                }
                assertThat(titles).containsExactly("Persist-C", "Persist-B", "Persist-A");
        }

        /**
         * Phase 7.4 (§6): structured key features — full lifecycle through the
         * real HTTP API against real MySQL (V9 migration): create with an
         * ordered list, persist across requests, reorder + edit by sending a
         * reordered list, null = leave unchanged, [] = clear, and reject
         * blank items with 400. Also asserts the PUBLIC surface returns the
         * ordered list.
         */
        @Test
        void keyFeatures_fullLifecycle_persistReorderClearAndValidate() throws Exception {
                // 1. Create with an ordered key-features list
                MvcResult created = mockMvc.perform(post("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "KeyFeature-Proj", "description": "Key features lifecycle project",
                                                 "visible": true, "displayOrder": 0, "skills": [],
                                                 "keyFeatures": ["Fast exports", "Multi-tenant safe", "NAG-01 compliant"]}
                                                """))
                                .andExpect(status().isCreated())
                                .andReturn();
                String uuid = objectMapper.readTree(created.getResponse().getContentAsString())
                                .path("data").path("uuid").asText();
                assertThat(uuid).isNotBlank();

                // Response keeps list order
                assertThat(objectMapper.readTree(created.getResponse().getContentAsString())
                                .path("data").path("keyFeatures").toString())
                                .isEqualTo("[\"Fast exports\",\"Multi-tenant safe\",\"NAG-01 compliant\"]");

                // 2. A BRAND-NEW GET (fresh persistence context) still shows them,
                //    and the PUBLIC surface carries the same ordered list
                MvcResult fetched = mockMvc.perform(get("/api/v1/me/projects/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(objectMapper.readTree(fetched.getResponse().getContentAsString())
                                .path("data").path("keyFeatures").toString())
                                .isEqualTo("[\"Fast exports\",\"Multi-tenant safe\",\"NAG-01 compliant\"]");

                MvcResult publicResult = mockMvc.perform(get("/api/v1/portfolios/{slug}/projects", slug))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(objectMapper.readTree(publicResult.getResponse().getContentAsString())
                                .path("data").get(0).path("keyFeatures").toString())
                                .isEqualTo("[\"Fast exports\",\"Multi-tenant safe\",\"NAG-01 compliant\"]");

                // 3. Reorder + edit in one step: send a reordered, edited list
                mockMvc.perform(put("/api/v1/me/projects/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "KeyFeature-Proj", "description": "Key features lifecycle project",
                                                 "visible": true, "displayOrder": 0, "skills": [],
                                                 "keyFeatures": ["NAG-01 compliant", "Edited feature",  "Fast exports"]}
                                                """))
                                .andExpect(status().isOk());

                MvcResult afterReorder = mockMvc.perform(get("/api/v1/me/projects/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(objectMapper.readTree(afterReorder.getResponse().getContentAsString())
                                .path("data").path("keyFeatures").toString())
                                .isEqualTo("[\"NAG-01 compliant\",\"Edited feature\",\"Fast exports\"]");

                // 4. null (field omitted) = leave unchanged
                mockMvc.perform(put("/api/v1/me/projects/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "KeyFeature-Proj", "description": "Key features lifecycle project",
                                                 "visible": true, "displayOrder": 0, "skills": []}
                                                """))
                                .andExpect(status().isOk());
                MvcResult afterNull = mockMvc.perform(get("/api/v1/me/projects/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andReturn();
                assertThat(objectMapper.readTree(afterNull.getResponse().getContentAsString())
                                .path("data").path("keyFeatures").toString())
                                .isEqualTo("[\"NAG-01 compliant\",\"Edited feature\",\"Fast exports\"]");

                // 5. Explicit empty list = clear all
                mockMvc.perform(put("/api/v1/me/projects/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "KeyFeature-Proj", "description": "Key features lifecycle project",
                                                 "visible": true, "displayOrder": 0, "skills": [], "keyFeatures": []}
                                                """))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/api/v1/me/projects/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                                .jsonPath("$.data.keyFeatures").isEmpty());

                // 6. Blank items rejected with 400 (bean validation)
                mockMvc.perform(put("/api/v1/me/projects/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "KeyFeature-Proj", "description": "Key features lifecycle project",
                                                 "visible": true, "displayOrder": 0, "skills": [],
                                                 "keyFeatures": ["ok", "   "]}
                                                """))
                                .andExpect(status().isBadRequest());
        }

        /**
         * Phase 7.4 (§11): visitor-stats race hardening.
         * Pins the ATOMIC increment semantics (V10 migration: unique key on
         * (type, ref_id) + single-statement UPDATE) — two increments must land
         * on ONE row and count exactly 2, with no lost update and no
         * duplicate row.
         */
        @Test
        // NOT_SUPPORTED suspends the class-level test transaction: the polls
        // below must run on plain auto-commit connections. When this test ran
        // transactional, JdbcTemplate participated in the thread-bound
        // transaction (DataSourceUtils), so every poll read from the same
        // REPEATABLE-READ snapshot taken before the async increments committed
        // and never saw them — the async writes were visible in the MySQL
        // binlog/log output yet invisible to the test.
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void visitorStats_incrementsAreAtomic_singleRowPerRef() throws Exception {
                // Two increments for the same (type, refId) — every public stat
                // increment is @Async, so poll the DB until both land.
                visitorStatsService.incrementTotalVisits();
                visitorStatsService.incrementTotalVisits();

                long count = 0;
                int rows = 0;
                long deadline = System.currentTimeMillis() + 10_000;
                while (System.currentTimeMillis() < deadline) {
                        Thread.sleep(200);
                        List<Long> counts = jdbcTemplate.queryForList(
                                        "SELECT `count` FROM visitor_stats WHERE type = 'TOTAL_VISITS' AND ref_id = 'site_total'",
                                        Long.class);
                        rows = counts.size();
                        if (rows == 1 && counts.get(0) == 2L) {
                                count = counts.get(0);
                                break;
                        }
                }

                org.assertj.core.api.Assertions.assertThat(rows)
                                .as("V10 unique key must keep exactly ONE row per (type, ref_id)")
                                .isEqualTo(1);
                org.assertj.core.api.Assertions.assertThat(count)
                                .as("both increments must count (old read-modify-write lost updates)")
                                .isEqualTo(2L);

                // The unique key itself must exist in the migrated schema.
                // NB: information_schema.STATISTICS has one row per INDEXED
                // COLUMN, so a two-column key must be counted by DISTINCT name.
                Integer ukCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS "
                                                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'visitor_stats' "
                                                + "AND INDEX_NAME = 'uk_visitor_stats_type_ref'",
                                Integer.class);
                org.assertj.core.api.Assertions.assertThat(ukCount).isEqualTo(1);
        }
}
