package com.forkmyfolio.controller.guest;

import com.forkmyfolio.advice.SkipApiResponseWrapper;
import com.forkmyfolio.aop.TrackVisitor;
import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ExportVisibility;
import com.forkmyfolio.model.enums.VisitorStatType;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.impl.MarkdownGenerationService;
import com.forkmyfolio.service.impl.PdfGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolios")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@Tag(name = "Public Portfolios", description = "Endpoints for viewing public user portfolios.")
@RequiredArgsConstructor
public class PortfolioDownloadController {

        private final PdfGenerationService pdfGenerationService;
        private final MarkdownGenerationService markdownGenerationService;
        private final PortfolioService portfolioService;
        private final UserService userService;

        /**
         * Phase 8 (PRV-1): resolves the export visibility contract for a
         * download request. Guests and authenticated non-owners always get
         * {@link ExportVisibility#PUBLIC} (identical filters to the public
         * portfolio pages); ONLY the authenticated owner themselves gets
         * {@link ExportVisibility#OWNER}. Authentication never grants
         * another user's hidden content. The ownership identity comparison
         * lives in {@link UserService} (NAG-01: controllers never touch
         * entity state directly).
         */
        private ExportVisibility resolveVisibility(User portfolioOwner) {
                return userService.isCurrentUser(portfolioOwner)
                                ? ExportVisibility.OWNER
                                : ExportVisibility.PUBLIC;
        }

        @GetMapping("/{slug}/pdf")
        @Operation(summary = "Download a user's portfolio as a PDF", description = "Generates and downloads a PDF version of the user's portfolio using a specified template.")
        @SkipApiResponseWrapper // This response is a file stream, not JSON, so we skip the standard wrapper.
        @TrackVisitor(VisitorStatType.PDF_DOWNLOAD)
        public ResponseEntity<byte[]> downloadPortfolioAsPdf(
                        @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe") @PathVariable String slug,
                        @Parameter(description = "The name of the PDF template to use.", example = "modern") @RequestParam(defaultValue = "modern") String template) {

                // FIX: Adhere to architectural rules. Controller fetches the entity, service
                // operates on it.
                User user = portfolioService.getPublicPortfolioUserBySlug(slug);
                PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generatePortfolioPdf(user, template,
                                resolveVisibility(user));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                                .filename(pdfFile.suggestedFilename())
                                .build());

                return ResponseEntity.ok()
                                .headers(headers)
                                .contentLength(pdfFile.content().length)
                                .body(pdfFile.content());
        }

        @GetMapping("/{slug}/markdown")
        @Operation(summary = "Download a user's portfolio as a Markdown file", description = "Generates and downloads a GitHub-friendly Markdown (.md) version of the user's portfolio.")
        @SkipApiResponseWrapper // This response is a file stream, not JSON
        @TrackVisitor(VisitorStatType.MARKDOWN_DOWNLOAD)
        public ResponseEntity<byte[]> downloadPortfolioAsMarkdown(
                        @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe") @PathVariable String slug) {

                // FIX: Adhere to architectural rules. Controller fetches the entity, service
                // operates on it.
                User user = portfolioService.getPublicPortfolioUserBySlug(slug);
                MarkdownGenerationService.MarkdownFile mdFile = markdownGenerationService
                                .generatePortfolioMarkdown(user, resolveVisibility(user));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"));
                headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                                .filename(mdFile.suggestedFilename())
                                .build());

                return ResponseEntity.ok()
                                .headers(headers)
                                .contentLength(mdFile.content().length)
                                .body(mdFile.content());
        }

        /**
         * Phase 0.19: downloads the owner's cover letter as its own PDF
         * (owner-only export toggle "Separate download tile").
         *
         * <p>
         * PRV-1: cover letter content is owner-personal. Any authenticated
         * non-owner or guest is answered with an indistinguishable 404 (not
         * a 403, so existence/setting state is never enumerable through
         * probes). Valid owners who have not enabled SEPARATE mode, or who
         * have no cover letter text, also receive the same 404 from the
         * generation service.
         *
         * <p>
         * Deliberately NOT annotated {@code @TrackVisitor}: the tracking
         * aspect counts anonymous visitors only, and this endpoint is only
         * reachable by the authenticated owner (everyone else 404s), so an
         * annotation could never fire anyway.
         */
        @GetMapping("/{slug}/cover-letter")
        @Operation(summary = "Download the owner's cover letter as a PDF",
                description = "Owner-only export of the cover letter configured in Profile & Account. "
                        + "Optional 'company' and 'role' query parameters replace the "
                        + "{{company}} / {{role}} placeholders at download time.")
        @SkipApiResponseWrapper // This response is a file stream, not JSON, so we skip the standard wrapper.
        public ResponseEntity<byte[]> downloadCoverLetter(
                        @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe") @PathVariable String slug,
                        @Parameter(description = "Optional value replacing {{company}} placeholders.", example = "Acme Corp") @RequestParam(defaultValue = "") String company,
                        @Parameter(description = "Optional value replacing {{role}} placeholders.", example = "Backend Engineer") @RequestParam(defaultValue = "") String role) {

                // Architectural rule: controller fetches the entity, service operates on it.
                User user = portfolioService.getPublicPortfolioUserBySlug(slug);
                if (!userService.isCurrentUser(user)) {
                        throw new ResourceNotFoundException("Portfolio not found: " + slug);
                }

                PdfGenerationService.PdfFile pdfFile = pdfGenerationService.generateCoverLetterPdf(user,
                                company, role);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                                .filename(pdfFile.suggestedFilename())
                                .build());

                return ResponseEntity.ok()
                                .headers(headers)
                                .contentLength(pdfFile.content().length)
                                .body(pdfFile.content());
        }
}