package com.forkmyfolio.integration;

import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 0.19: cover letter export end-to-end suite (real MySQL
 * testcontainer, real JWT logins, real PDF text extraction).
 *
 * <p>
 * Contract under test:
 * <ul>
 *   <li>Default OFF: nobody can download the cover letter file, and no
 *       resume PDF / Markdown gains a cover letter.</li>
 *   <li>IN_PDF: the OWNER's resume PDF gains an appended cover letter
 *       page and the owner's Markdown gains a Cover Letter section.
 *       PUBLIC exports (guests AND other authenticated users) never
 *       contain it (PRV-1).</li>
 *   <li>SEPARATE: {@code GET /api/v1/portfolios/{slug}/cover-letter} is
 *       owner-only with optional {@code company}/{@code role} placeholder
 *       replacement. Guests, authenticated non-owners and owners in any
 *       other mode all receive an indistinguishable 404.</li>
 * </ul>
 */
@Transactional
public class CoverLetterEndpointsIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String LETTER_TEXT = "To Whom It May Concern-" + "coverlettermortar";

    @Autowired
    private UserRepository userRepository;

    private String ownerEmail;
    private String ownerSlug;
    private String ownerJwt;
    private String attackerJwt;

    private String letterCompany = "Inte-grit-e-Corp " + System.nanoTime() % 100000;

    private String letterText() {
        return "Dear {{company}}, I is applying for {{role}}. Marker: " + LETTER_TEXT;
    }

    @BeforeEach
    void seedOwnerWithCoverLetter() throws Exception {
        String suffix = runSuffix();
        ownerEmail = "cover-owner-" + suffix + "@example.com";
        ownerSlug = "cover-owner-" + suffix;

        User owner = seedUser(ownerEmail, ownerSlug, true);
        owner.getPortfolioProfile().setCoverLetterTemplate(letterText());
        userRepository.save(owner);

        User attacker = seedUser("cover-attacker-" + suffix + "@example.com",
                "cover-attacker-" + suffix, true);

        ownerJwt = realLogin(ownerEmail, PASSWORD);
        attackerJwt = realLogin(attacker.getEmail(), PASSWORD);
    }

    // ------------------------------------------------------------------
    // Default OFF
    // ------------------------------------------------------------------

    @Test
    void defaultOff_standaloneDownloadIs404_forEveryone() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/{slug}/cover-letter", ownerSlug)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/portfolios/{slug}/cover-letter", ownerSlug))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/portfolios/{slug}/cover-letter", ownerSlug)
                        .header("Authorization", "Bearer " + attackerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void defaultOff_resumeExportsContainNoCoverLetter() throws Exception {
        assertThat(downloadPdfText(ownerJwt)).doesNotContain(LETTER_TEXT);
        assertThat(downloadMarkdown(ownerJwt)).doesNotContain("## ✉️ Cover Letter");
        assertThat(downloadMarkdown(null)).doesNotContain("## ✉️ Cover Letter");
    }

    // ------------------------------------------------------------------
    // IN_PDF: appended page + Markdown section, PUBLIC untouched
    // ------------------------------------------------------------------

    @Test
    void inPdf_ownerResumeGainsCoverLetterPage_andMarkdownSection() throws Exception {
        setMode("IN_PDF");

        String ownerMd = downloadMarkdown(ownerJwt);
        assertThat(ownerMd).contains("## ✉️ Cover Letter");
        assertThat(ownerMd).contains(LETTER_TEXT);
        assertThat(ownerMd).contains("{{company}}"); // placeholders emitted verbatim in Markdown

        String ownerPdfText = downloadPdfText(ownerJwt);
        assertThat(ownerPdfText).contains("Cover Letter");
        assertThat(ownerPdfText.replaceAll("\\s+", " ")).contains(LETTER_TEXT);
    }

    @Test
    void inPdf_publicExportsNeverContainTheCoverLetter() throws Exception {
        setMode("IN_PDF");

        // Guest
        assertThat(downloadPdfText(null)).doesNotContain("Cover Letter");
        assertThat(downloadPdfText(null)).doesNotContain(LETTER_TEXT);
        assertThat(downloadMarkdown(null)).doesNotContain("## ✉️ Cover Letter");

        // Authenticated non-owner
        assertThat(downloadPdfText(attackerJwt)).doesNotContain(LETTER_TEXT);
        assertThat(downloadMarkdown(attackerJwt)).doesNotContain("## ✉️ Cover Letter");
    }

    // ------------------------------------------------------------------
    // SEPARATE: owner-only standalone download
    // ------------------------------------------------------------------

    @Test
    void separate_ownerDownloadsStandaloneCoverLetterPdf_withPlaceholdersReplaced() throws Exception {
        setMode("SEPARATE");
        String marker = "Sentinel-" + System.nanoTime() % 100000;
        String company = letterCompany;
        String role = "Backend Wizard " + marker;

        MvcResult result = mockMvc.perform(get("/api/v1/portfolios/{slug}/cover-letter", ownerSlug)
                        .param("company", company)
                        .param("role", role)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andReturn();
        byte[] pdf = result.getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).startsWith("%PDF");
        assertThat(pdf.length).isGreaterThan(1000);
        String text = extractPdfText(pdf);
        // PDF text extraction inserts line breaks mid-paragraph; compare on
        // a whitespace-collapsed view so assertions read like a human does.
        String flat = text.replaceAll("\\s+", " ");
        assertThat(flat).contains("Dear " + company + ", I is applying for " + role + ".");
        assertThat(flat).contains(LETTER_TEXT);
        // Placeholder tokens are replaced, not echoed
        assertThat(flat).doesNotContain("{{company}}");
        assertThat(flat).doesNotContain("{{role}}");

        // Content-Disposition carries a cover-letter filename
        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertThat(disposition).contains("CoverLetter").contains(".pdf");
    }

    @Test
    void separate_unfilledPlaceholdersStayVerbatim_whenParamsOmitted() throws Exception {
        setMode("SEPARATE");

        MvcResult result = mockMvc.perform(get("/api/v1/portfolios/{slug}/cover-letter", ownerSlug)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andReturn();
        String text = extractPdfText(result.getResponse().getContentAsByteArray());
        String flat = text.replaceAll("\\s+", " ");
        assertThat(flat).contains("Dear {{company}}, I is applying for {{role}}.");
    }

    @Test
    void separate_guestsAndNonOwnersGetIndistinguishable404() throws Exception {
        setMode("SEPARATE");

        mockMvc.perform(get("/api/v1/portfolios/{slug}/cover-letter", ownerSlug))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/portfolios/{slug}/cover-letter", ownerSlug)
                        .header("Authorization", "Bearer " + attackerJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void separate_modeGatesStandaloneDownload_only() throws Exception {
        setMode("IN_PDF");
        // IN_PDF only touches the resume; the standalone file stays closed
        mockMvc.perform(get("/api/v1/portfolios/{slug}/cover-letter", ownerSlug)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** PUTs export.coverletter.mode with a fresh real login (self-contained). */
    private void setMode(String mode) throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/v1/me/settings")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andReturn();
        String body = listResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = JsonPath.read(body,
                "$.data[?(@.name=='export.coverletter.mode')]");
        assertThat(matches).as("export.coverletter.mode setting present").hasSize(1);

        String payload = objectMapper.writeValueAsString(List.of(Map.of(
                "uuid", matches.get(0).get("uuid"),
                "value", mode)));
        mockMvc.perform(put("/api/v1/me/settings")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private String downloadMarkdown(String jwt) throws Exception {
        var request = get("/api/v1/portfolios/{slug}/markdown", ownerSlug);
        if (jwt != null) {
            request = request.header("Authorization", "Bearer " + jwt);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String downloadPdfText(String jwt) throws Exception {
        var request = get("/api/v1/portfolios/{slug}/pdf", ownerSlug)
                .param("template", "classic");
        if (jwt != null) {
            request = request.header("Authorization", "Bearer " + jwt);
        }
        byte[] pdfBytes = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        return extractPdfText(pdfBytes);
    }

    private String extractPdfText(byte[] pdfBytes) throws Exception {
        StringBuilder extracted = new StringBuilder();
        var reader = new org.openpdf.text.pdf.PdfReader(pdfBytes);
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
}
