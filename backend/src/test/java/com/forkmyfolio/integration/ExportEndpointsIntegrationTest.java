package com.forkmyfolio.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Export correctness (PDF and Markdown) against REAL generated output:
 * the PDF must be a real iText PDF file (magic bytes + size), and the
 * Markdown export must contain the portfolio's actual content.
 */
@Transactional
public class ExportEndpointsIntegrationTest extends AbstractMySqlIntegrationTest {

        private static final String PASSWORD = "Password123!";

        private String email;
        private String slug;
        private String projectTitle;
        private String experienceTitle;

        @BeforeEach
        void seedPortfolio() throws Exception {
                String suffix = runSuffix();
                email = "export-" + suffix + "@example.com";
                slug = "export-" + suffix;
                projectTitle = "Exported Project " + suffix;
                experienceTitle = "Exported Experience " + suffix;

                seedUser(email, slug, true);
                String jwt = realLogin(email, PASSWORD);

                mockMvc.perform(post("/api/v1/me/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"title": "%s", "description": "Project exported to files %s",
                                                 "visible": true, "displayOrder": 0, "skills": [],
                                                 "keyFeatures": ["Export-proof feature one", "Export-proof feature two"]}
                                                """.formatted(projectTitle, suffix)))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/v1/me/experiences")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType("application/json")
                                .content("""
                                                {"jobTitle": "%s", "companyName": "Export Corp",
                                                 "startDate": "2020-01-01", "endDate": "2022-06-30",
                                                 "description": "Built things for the export test.",
                                                 "visible": true, "displayOrder": 0, "skills": []}
                                                """.formatted(experienceTitle)))
                                .andExpect(status().isCreated());
        }

        @Test
        void pdfDownload_isARealPdf() throws Exception {
                byte[] pdf = mockMvc.perform(get("/api/v1/portfolios/{slug}/pdf", slug))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsByteArray();

                assertThat(pdf.length)
                                .as("a real rendered PDF is never a few bytes")
                                .isGreaterThan(2000);
                assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                                .as("PDF magic header")
                                .startsWith("%PDF");
        }

        @Test
        void markdownDownload_containsPortfolioContent() throws Exception {
                String markdown = mockMvc.perform(get("/api/v1/portfolios/{slug}/markdown", slug))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                assertThat(markdown).isNotBlank();
                assertThat(markdown).contains(projectTitle);
                assertThat(markdown).contains(experienceTitle);

                // Phase 7.4 (§6): structured key features render as ordered sub-bullets
                assertThat(markdown).contains("**Key Features:**");
                assertThat(markdown).contains("  - Export-proof feature one");
                assertThat(markdown).contains("  - Export-proof feature two");
        }

        /**
         * Phase 7.4 (§6/§8): the downloaded PDF (default template resolution
         * path) must carry the structured key features in its rendered text.
         * Cross-template coverage lives in PdfGenerationServiceTest.
         */
        @Test
        void pdfDownload_containsKeyFeatures() throws Exception {
                byte[] pdf = mockMvc.perform(get("/api/v1/portfolios/{slug}/pdf", slug))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsByteArray();

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
                String text = extracted.toString();
                assertThat(text).contains("Key Features");
                assertThat(text).contains("Export-proof feature one");
                assertThat(text).contains("Export-proof feature two");
        }
}
