package com.forkmyfolio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 0.29 (TASKS §G): the owner's eight required regression tests for
 * experience ordering, driven through the REAL HTTP API against real MySQL.
 *
 * <p>Coverage map (owner brief §4 "Required Regression Tests"):
 * <ol>
 *   <li>changing multiple experience positions in one editing session;</li>
 *   <li>moving an entry from position 100 to position 4;</li>
 *   <li>reopening the editor and verifying the positions persisted;</li>
 *   <li>reloading the application and verifying ordering remains unchanged;</li>
 *   <li>assigning position 0 and verifying chronological placement;</li>
 *   <li>handling overlapping employment periods;</li>
 *   <li>manual ordering is not unintentionally overwritten by automatic
 *       chronological sorting;</li>
 *   <li>all relevant portfolio views and exports respect the resulting
 *       order (owner API, public API, full portfolio payload, Markdown
 *       export, PDF export).</li>
 * </ol>
 *
 * <p>Also pins the root-cause fix: an experience WITHOUT skills used to
 * return HTTP 500 from {@code PUT /me/experiences/{uuid}}
 * ({@code UnsupportedOperationException} from an immutable {@code Set.of()}
 * handed to Hibernate's merge), rolling back the whole transaction and
 * silently discarding the position change. Every PUT below sends
 * {@code "skills": []} — the exact payload that used to fail.
 */
@Transactional
public class ExperienceOrderingIntegrationTest extends AbstractMySqlIntegrationTest {

        private static final String PASSWORD = "Password123!";

        private String email;
        private String slug;
        private String jwt;

        @org.springframework.beans.factory.annotation.Autowired
        private jakarta.persistence.EntityManager entityManager;

        @BeforeEach
        void seedUserAndLogin() throws Exception {
                String suffix = runSuffix();
                email = "exporder-" + suffix + "@example.com";
                slug = "exporder-" + suffix;
                seedUser(email, slug, true);
                jwt = realLogin(email, PASSWORD);
        }

        // ------------------------------------------------------------------
        // Helpers
        // ------------------------------------------------------------------

        private String createExperience(String jobTitle, int displayOrder, String startDate, String endDate)
                        throws Exception {
                String endJson = endDate == null ? "null" : "\"" + endDate + "\"";
                MvcResult created = mockMvc.perform(post("/api/v1/me/experiences")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"jobTitle": "%s", "companyName": "Ordering Corp",
                                                 "startDate": "%s", "endDate": %s,
                                                 "description": "Section G ordering scenario.",
                                                 "visible": true, "displayOrder": %d, "skills": []}
                                                """.formatted(jobTitle, startDate, endJson, displayOrder)))
                                .andExpect(status().isCreated())
                                .andReturn();
                return objectMapper.readTree(created.getResponse().getContentAsString())
                                .path("data").path("uuid").asText();
        }

        /**
         * Full-object PUT exactly as the admin SPA builds it — including the
         * empty skills array that used to trigger the 500/rollback defect.
         */
        private void updateExperience(String uuid, String jobTitle, int displayOrder, String startDate,
                        String endDate) throws Exception {
                String endJson = endDate == null ? "null" : "\"" + endDate + "\"";
                mockMvc.perform(put("/api/v1/me/experiences/{uuid}", uuid)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"jobTitle": "%s", "companyName": "Ordering Corp",
                                                 "startDate": "%s", "endDate": %s,
                                                 "description": "Section G ordering scenario.",
                                                 "visible": true, "displayOrder": %d, "skills": []}
                                                """.formatted(jobTitle, startDate, endJson, displayOrder)))
                                .andExpect(status().isOk());
        }

        /** Fresh owner-surface read: title -> displayOrder, in served order. */
        private Map<String, Integer> ownerPositions() throws Exception {
                return positionsOf(fetchJson("/api/v1/me/experiences", true));
        }

        private List<String> ownerTitles() throws Exception {
                return new ArrayList<>(ownerPositions().keySet());
        }

        /** Public guest experience endpoint, in served order. */
        private List<String> publicExperienceTitles() throws Exception {
                return new ArrayList<>(positionsOf(
                                fetchJson("/api/v1/portfolios/" + slug + "/experience", false)).keySet());
        }

        /** Full public portfolio payload: $.data.experiences, in served order. */
        private List<String> publicPortfolioTitles() throws Exception {
                // Simulate a genuine application reload. The class-level test
                // transaction keeps ONE Hibernate session alive for the whole
                // test, so the user's lazy `experiences` set can hold a stale
                // pre-create snapshot (inverse-side mappedBy collections are
                // not refreshed on flush). Production serves every request
                // from a fresh session; em.clear() reproduces that here.
                entityManager.flush();
                entityManager.clear();

                MvcResult result = mockMvc.perform(get("/api/v1/portfolios/" + slug))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                List<String> titles = new ArrayList<>();
                for (JsonNode experience : root.path("data").path("experiences")) {
                        titles.add(experience.path("jobTitle").asText());
                }
                return titles;
        }

        private JsonNode fetchJson(String path, boolean authenticated) throws Exception {
                var request = get(path);
                if (authenticated) {
                        request = request.header("Authorization", "Bearer " + jwt);
                }
                MvcResult result = mockMvc.perform(request)
                                .andExpect(status().isOk())
                                .andReturn();
                return objectMapper.readTree(result.getResponse().getContentAsString());
        }

        private static Map<String, Integer> positionsOf(JsonNode root) {
                Map<String, Integer> positions = new LinkedHashMap<>();
                JsonNode array = root.path("data");
                if (!array.isArray()) {
                        array = root;
                }
                for (JsonNode node : array) {
                        positions.put(node.path("jobTitle").asText(), node.path("displayOrder").asInt());
                }
                return positions;
        }

        private static String extractPdfText(byte[] pdf) throws java.io.IOException {
                StringBuilder extracted = new StringBuilder();
                var reader = new org.openpdf.text.pdf.PdfReader(pdf);
                try {
                        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                                extracted.append(new org.openpdf.text.pdf.parser.PdfTextExtractor(reader)
                                                .getTextFromPage(i))
                                                .append('\n');
                        }
                } finally {
                        reader.close();
                }
                return extracted.toString();
        }

        /** Asserts a < b < c as character positions inside a document. */
        private static void assertDocumentOrder(String document, String a, String b, String c) {
                int ia = document.indexOf(a);
                int ib = document.indexOf(b);
                int ic = document.indexOf(c);
                assertThat(ia).as("'%s' must appear in the document", a).isGreaterThanOrEqualTo(0);
                assertThat(ib).as("'%s' must appear in the document", b).isGreaterThan(ia);
                assertThat(ic).as("'%s' must appear in the document", c).isGreaterThan(ib);
        }

        // ------------------------------------------------------------------
        // Owner tests 1 + 3: one editing session, multiple positions, reopen
        // ------------------------------------------------------------------

        @Test
        void multiplePositionChangesInOneEditingSession_persistAndSurviveReopen() throws Exception {
                // The owner's exact scenario: 4 -> 5, 6 -> 7, 100 -> 4.
                String alpha = createExperience("Alpha Role", 4, "2024-02-01", null);
                String beta = createExperience("Beta Role", 6, "2022-01-01", "2023-12-31");
                String gamma = createExperience("Gamma Role", 100, "2019-06-01", "2021-08-31");

                // --- One editing session: three full-object saves in a row ---
                updateExperience(alpha, "Alpha Role", 5, "2024-02-01", null);
                updateExperience(beta, "Beta Role", 7, "2022-01-01", "2023-12-31");
                updateExperience(gamma, "Gamma Role", 4, "2019-06-01", "2021-08-31");

                // --- Reopen the editor (brand-new GET): all three persisted ---
                Map<String, Integer> reopened = ownerPositions();
                assertThat(reopened)
                                .as("every position from the session must persist (root cause: an "
                                                + "empty-skills PUT used to 500 and roll back the save)")
                                .containsEntry("Alpha Role", 5)
                                .containsEntry("Beta Role", 7)
                                .containsEntry("Gamma Role", 4);

                // Served order follows the persisted slots: 4, 5, 7.
                assertThat(ownerTitles()).containsExactly("Gamma Role", "Alpha Role", "Beta Role");
        }

        // ------------------------------------------------------------------
        // Owner test 2: 100 -> 4 must not clobber unrelated positions
        // ------------------------------------------------------------------

        @Test
        void moveFrom100To4_leavesOtherPositionsUntouched() throws Exception {
                String retail = createExperience("Retail Role", 100, "2019-06-01", "2021-08-31");
                String anchor = createExperience("Anchor Role", 11, "2024-03-01", null);

                updateExperience(retail, "Retail Role", 4, "2019-06-01", "2021-08-31");

                Map<String, Integer> after = ownerPositions();
                assertThat(after)
                                .containsEntry("Retail Role", 4)
                                .as("an unrelated entry's position must not be reset by another entry's save")
                                .containsEntry("Anchor Role", 11);
                assertThat(ownerTitles()).containsExactly("Retail Role", "Anchor Role");
        }

        // ------------------------------------------------------------------
        // Owner tests 3 + 4: reopen + reload stability
        // ------------------------------------------------------------------

        @Test
        void reopenedEditorAndReloadedApplication_showIdenticalOrdering() throws Exception {
                String one = createExperience("First Role", 2, "2024-01-01", null);
                String two = createExperience("Second Role", 8, "2022-01-01", "2023-01-01");
                String three = createExperience("Third Role", 5, "2020-01-01", "2021-01-01");

                updateExperience(one, "First Role", 9, "2024-01-01", null);
                updateExperience(two, "Second Role", 1, "2022-01-01", "2023-01-01");
                updateExperience(three, "Third Role", 6, "2020-01-01", "2021-01-01");

                // Three independent reads (reopen, reload, reload) — identical
                // positions AND identical served order, every time.
                Map<String, Integer> read1 = ownerPositions();
                Map<String, Integer> read2 = ownerPositions();
                Map<String, Integer> read3 = ownerPositions();

                assertThat(read1).containsEntry("First Role", 9)
                                .containsEntry("Second Role", 1)
                                .containsEntry("Third Role", 6);
                assertThat(read2).isEqualTo(read1);
                assertThat(read3).isEqualTo(read1);

                List<String> expectedOrder = List.of("Second Role", "Third Role", "First Role");
                assertThat(ownerTitles()).isEqualTo(expectedOrder);
                assertThat(publicExperienceTitles()).isEqualTo(expectedOrder);
                assertThat(publicPortfolioTitles()).isEqualTo(expectedOrder);
        }

        // ------------------------------------------------------------------
        // Owner test 5: position 0 = automatic chronological placement
        // ------------------------------------------------------------------

        @Test
        void positionZero_sortsChronologically_forCurrentAndHistoricalEntries() throws Exception {
                // Manual pins (deliberately NOT date-sorted — pins must win)...
                createExperience("Manual Now", 10, "2024-01-01", null);
                createExperience("Manual Past", 20, "2020-01-01", "2021-01-31");
                // ...and two automatic entries whose dates decide their slots.
                createExperience("Auto Mid", 0, "2022-06-01", "2022-12-31");
                createExperience("Auto Fresh", 0, "2025-03-01", null);

                Map<String, Integer> positions = ownerPositions();

                // Position 0 persisted AS 0 (never normalised to something else)...
                assertThat(positions)
                                .containsEntry("Auto Mid", 0)
                                .containsEntry("Auto Fresh", 0)
                                .containsEntry("Manual Now", 10)
                                .containsEntry("Manual Past", 20);

                // ...and the served order interleaves the automatic entries by
                // date while the pins keep their relative order:
                // Auto Fresh (2025) -> Manual Now (2024) -> Auto Mid (2022) ->
                // Manual Past (2020).
                List<String> expected = List.of("Auto Fresh", "Manual Now", "Auto Mid", "Manual Past");
                assertThat(ownerTitles()).isEqualTo(expected);
                assertThat(publicExperienceTitles()).isEqualTo(expected);
                assertThat(publicPortfolioTitles()).isEqualTo(expected);
        }

        // ------------------------------------------------------------------
        // Owner test 6: overlapping employment periods
        // ------------------------------------------------------------------

        @Test
        void overlappingEmploymentPeriods_orderDeterministicallyOnEveryRead() throws Exception {
                createExperience("Long Job", 0, "2022-01-01", "2023-06-30");
                createExperience("Overlap Job", 0, "2022-05-01", "2022-12-31");
                createExperience("Now Job", 0, "2025-01-01", null);

                List<String> expected = List.of("Now Job", "Overlap Job", "Long Job");

                // Same result across repeated owner reads and the public surface.
                assertThat(ownerTitles()).isEqualTo(expected);
                assertThat(ownerTitles()).isEqualTo(expected);
                assertThat(publicExperienceTitles()).isEqualTo(expected);
                assertThat(publicPortfolioTitles()).isEqualTo(expected);
        }

        // ------------------------------------------------------------------
        // Owner test 7: manual ordering never overwritten by auto sorting
        // ------------------------------------------------------------------

        @Test
        void manualOrder_isNeverOverwrittenByAutomaticSorting() throws Exception {
                // Anti-chronological pins (older entry pinned FIRST) + one
                // automatic entry: the automatic sort must not reorder the pins.
                String pinOld = createExperience("Pin Old", 10, "2020-01-01", "2021-01-01");
                String pinNew = createExperience("Pin New", 20, "2024-01-01", null);
                String auto = createExperience("Auto Slot", 0, "2022-06-01", "2022-12-31");

                // Simulate an edit session that touches ONE item (full-object save).
                updateExperience(pinNew, "Pin New", 20, "2024-01-01", null);
                // ...and one that touches the automatic entry (still 0).
                updateExperience(auto, "Auto Slot", 0, "2022-06-01", "2022-12-31");

                Map<String, Integer> positions = ownerPositions();

                // Pin values are untouched (no server-side normalisation)...
                assertThat(positions)
                                .containsEntry("Pin Old", 10)
                                .containsEntry("Pin New", 20)
                                .containsEntry("Auto Slot", 0);

                // ...and the pinned relative order survives every read, with
                // the automatic entry placed by date at the first position
                // whose entry is older than it.
                List<String> expected = List.of("Auto Slot", "Pin Old", "Pin New");
                assertThat(ownerTitles()).isEqualTo(expected);
                assertThat(ownerTitles()).isEqualTo(expected);
                assertThat(publicExperienceTitles()).isEqualTo(expected);
        }

        // ------------------------------------------------------------------
        // Owner test 8: every view and export respects the resulting order
        // ------------------------------------------------------------------

        @Test
        void allViewsAndExports_respectTheResultingOrder() throws Exception {
                // Establish the owner's canonical session order: 4 -> 5, 6 -> 7,
                // 100 -> 4 (serves as Gamma, Alpha, Beta).
                String alpha = createExperience("Alpha Role", 4, "2024-02-01", null);
                String beta = createExperience("Beta Role", 6, "2022-01-01", "2023-12-31");
                String gamma = createExperience("Gamma Role", 100, "2019-06-01", "2021-08-31");
                updateExperience(alpha, "Alpha Role", 5, "2024-02-01", null);
                updateExperience(beta, "Beta Role", 7, "2022-01-01", "2023-12-31");
                updateExperience(gamma, "Gamma Role", 4, "2019-06-01", "2021-08-31");

                List<String> expected = List.of("Gamma Role", "Alpha Role", "Beta Role");

                // 1. Owner API (admin list + editor reopen).
                assertThat(ownerTitles()).isEqualTo(expected);

                // 2. Public guest experience endpoint.
                assertThat(publicExperienceTitles()).isEqualTo(expected);

                // 3. Full public portfolio payload (Nuxt reads this one).
                assertThat(publicPortfolioTitles()).isEqualTo(expected);

                // 4. Markdown export.
                MvcResult markdown = mockMvc.perform(get("/api/v1/portfolios/{slug}/markdown", slug))
                                .andExpect(status().isOk())
                                .andReturn();
                assertDocumentOrder(markdown.getResponse().getContentAsString(StandardCharsets.UTF_8),
                                "Gamma Role", "Alpha Role", "Beta Role");

                // 5. PDF export (every template resolves through the same
                //    ExperienceOrdering contract — see the template unit tests).
                byte[] pdf = mockMvc.perform(get("/api/v1/portfolios/{slug}/pdf", slug))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsByteArray();
                assertThat(pdf.length).as("a real rendered PDF is never a few bytes").isGreaterThan(2000);
                assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).startsWith("%PDF");
                assertDocumentOrder(extractPdfText(pdf),
                                "Gamma Role", "Alpha Role", "Beta Role");
        }
}
