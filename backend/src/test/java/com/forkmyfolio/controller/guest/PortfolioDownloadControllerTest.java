
package com.forkmyfolio.controller.guest;

import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ExportVisibility;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.impl.MarkdownGenerationService;
import com.forkmyfolio.service.impl.PdfGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PortfolioDownloadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PdfGenerationService pdfGenerationService;

    @Mock
    private MarkdownGenerationService markdownGenerationService;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PortfolioDownloadController portfolioDownloadController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioDownloadController)
                .addPlaceholderValue("app.cors.allowed-origins", "http://localhost:3000")
                .build();
    }

    @Test
    void downloadPortfolioAsPdf_shouldReturnPdfFile() throws Exception {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        byte[] pdfContent = "test-pdf-content".getBytes(StandardCharsets.UTF_8);
        PdfGenerationService.PdfFile pdfFile = new PdfGenerationService.PdfFile(pdfContent, "portfolio.pdf");

        when(portfolioService.getPublicPortfolioUserBySlug(anyString())).thenReturn(user);
        // Anonymous request (standalone MockMvc has no security context) must
        // resolve to PUBLIC visibility — Phase 8 (PRV-1).
        when(userService.isCurrentUser(any(User.class))).thenReturn(false);
        when(pdfGenerationService.generatePortfolioPdf(any(User.class), anyString(), eq(ExportVisibility.PUBLIC)))
                .thenReturn(pdfFile);

        mockMvc.perform(get("/api/v1/portfolios/john-doe/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"portfolio.pdf\""))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void downloadPortfolioAsMarkdown_shouldReturnMarkdownFile() throws Exception {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        byte[] mdContent = "test-md-content".getBytes(StandardCharsets.UTF_8);
        MarkdownGenerationService.MarkdownFile mdFile = new MarkdownGenerationService.MarkdownFile(mdContent,
                "portfolio.md");

        when(portfolioService.getPublicPortfolioUserBySlug(anyString())).thenReturn(user);
        when(userService.isCurrentUser(any(User.class))).thenReturn(false);
        when(markdownGenerationService.generatePortfolioMarkdown(any(User.class), eq(ExportVisibility.PUBLIC)))
                .thenReturn(mdFile);

        mockMvc.perform(get("/api/v1/portfolios/jane-doe/markdown"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/markdown;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"portfolio.md\""))
                .andExpect(content().string(is(new String(mdContent, StandardCharsets.UTF_8))));
    }

    /**
     * Phase 8 (PRV-1): when the authenticated principal IS the portfolio
     * owner, the download must be generated with OWNER visibility (the
     * owner may export their own hidden content).
     */
    @Test
    void downloadPortfolioAsPdf_ownerRequest_resolvesOwnerVisibility() throws Exception {
        User owner = new User();
        owner.setFirstName("Jane");
        owner.setLastName("Doe");
        owner.setEmail("jane@example.com");
        byte[] pdfContent = "owner-pdf-content".getBytes(StandardCharsets.UTF_8);
        PdfGenerationService.PdfFile pdfFile = new PdfGenerationService.PdfFile(pdfContent, "owner.pdf");

        when(portfolioService.getPublicPortfolioUserBySlug(anyString())).thenReturn(owner);
        when(userService.isCurrentUser(owner)).thenReturn(true);
        when(pdfGenerationService.generatePortfolioPdf(any(User.class), anyString(), eq(ExportVisibility.OWNER)))
                .thenReturn(pdfFile);

        mockMvc.perform(get("/api/v1/portfolios/jane-doe/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfContent));
    }

    /**
     * Phase 8 (PRV-1): an authenticated user who is NOT the owner must be
     * treated exactly like a guest — PUBLIC visibility, never OWNER.
     */
    @Test
    void downloadPortfolioAsMarkdown_nonOwnerAuthenticatedUser_resolvesPublicVisibility() throws Exception {
        User owner = new User();
        owner.setFirstName("Jane");
        owner.setLastName("Doe");
        owner.setEmail("jane@example.com");
        byte[] mdContent = "public-md-content".getBytes(StandardCharsets.UTF_8);
        MarkdownGenerationService.MarkdownFile mdFile = new MarkdownGenerationService.MarkdownFile(mdContent,
                "portfolio.md");

        when(portfolioService.getPublicPortfolioUserBySlug(anyString())).thenReturn(owner);
        when(userService.isCurrentUser(owner)).thenReturn(false);
        when(markdownGenerationService.generatePortfolioMarkdown(any(User.class), eq(ExportVisibility.PUBLIC)))
                .thenReturn(mdFile);

        mockMvc.perform(get("/api/v1/portfolios/jane-doe/markdown"))
                .andExpect(status().isOk())
                .andExpect(content().string(is(new String(mdContent, StandardCharsets.UTF_8))));
    }
}
