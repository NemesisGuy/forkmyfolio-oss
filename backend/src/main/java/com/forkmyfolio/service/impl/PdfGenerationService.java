package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.*;
import com.forkmyfolio.model.enums.ExportVisibility;
import com.forkmyfolio.repository.SettingRepository;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.pdf.PortfolioData;
import com.forkmyfolio.service.pdf.templates.*;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfGenerationService {


    //<editor-fold desc="Color & Font Constants">
    // These are public so templates can access them easily.
    public static final Color PRIMARY_COLOR = new Color(34, 49, 63);
    public static final Color SECONDARY_COLOR = new Color(128, 128, 128);
    public static final Color ACCENT_COLOR = new Color(52, 152, 219);
    public static final Color SIDEBAR_BG = new Color(245, 245, 245);
    public static final Color SIDEBAR_TEXT = new Color(51, 51, 51);
    public static final Color SKILL_EXPERT_COLOR = new Color(22, 160, 133);      // #16A085 (Teal Green)
    //</editor-fold>
    public static final Color SKILL_ADVANCED_COLOR = new Color(52, 152, 219);    // #3498DB (Soft Blue)
    public static final Color SKILL_INTERMEDIATE_COLOR = new Color(241, 196, 15); // #F1C40F (Golden Yellow)
    public static final Color SKILL_BEGINNER_COLOR = new Color(231, 76, 60);     // #E74C3C (Soft Red)
    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);
    //<editor-fold desc="Dependencies">
    // Phase 8 (PRV-1): section/content selection is centralized in the
    // shared ExportContentService; this service only resolves the template
    // and renders the resulting PortfolioData.
    private final ExportContentService exportContentService;
    private final SettingRepository settingRepository;
    private final UserService userService;

    //</editor-fold>
    // A map to hold available templates. This makes adding new ones easy.
    // Phase 8 (8.2): "metro-dark" was REMOVED from the selectable registry
    // (canvas-wide background fill renders unreliably across PDF viewers and
    // printers). Existing users with a saved "metro-dark" preference are
    // transparently aliased to "metro-light" at resolution time — never a
    // hard failure. The MetroDarkTemplate class file is retained so
    // previously generated artefacts remain reproducible.
    private final Map<String, PortfolioPdfTemplate> templates = Map.of(
            "modern", newTemplate(ModernTemplate::new),
            "elegance", newTemplate(EleganceTemplate::new),
            "classic", newTemplate(ClassicTemplate::new),
            "metro-light", newTemplate(MetroLightTemplate::new),
            "executive", newTemplate(ExecutiveTemplate::new),
            "modern-minimal", newTemplate(ModernMinimalTemplate::new),
            "technical", newTemplate(TechnicalTemplate::new),
            "business-card", new BusinessCardTemplate()
    );

    /**
     * Phase 8 (8.2): constructs a template whose font setup happens in the
     * constructor (checked {@link IOException}). A failure here is a
     * deployment-time configuration error, not a per-request concern, so it
     * fails loudly at bean construction.
     */
    private static PortfolioPdfTemplate newTemplate(TemplateFactory factory) {
        try {
            return factory.create();
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Failed to initialise PDF template", e);
        }
    }

    @FunctionalInterface
    private interface TemplateFactory {
        PortfolioPdfTemplate create() throws IOException, DocumentException;
    }

    private static BaseFont loadOptionalFontFromResource(String resourcePath) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Font resource not found on classpath: '{}'. Icons may not display.", resourcePath);
                return null;
            }
            byte[] fontBytes = is.readAllBytes();
            // Phase 0.12 (OpenPDF migration): byte[]-based embedded font
            // creation; the resource path doubles as the cache key. IDENTITY_H
            // keeps the icon code points (private-use area) renderable.
            return BaseFont.createFont(resourcePath, BaseFont.IDENTITY_H, true, false, fontBytes, null);
        } catch (IOException | DocumentException e) {
            log.error("Failed to load font resource '{}': {}", resourcePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Returns a list of available template names.
     *
     * @return A list of strings representing the template keys.
     */
    public List<String> getAvailableTemplateNames() {
        return new ArrayList<>(templates.keySet());
    }

    /**
     * Generates a portfolio PDF for a specific user using a specified template.
     *
     * @param user         The User entity for whom the portfolio is being generated.
     * @param templateName The name of the template to use (e.g., "modern").
     * @param visibility   Phase 8 (PRV-1): the visibility contract for content
     *                     selection — PUBLIC mirrors the public portfolio pages'
     *                     filters (guests and non-owner requesters), OWNER includes
     *                     the owner's own non-public content. Resolved by the
     *                     controller; never derived here.
     * @return A PdfFile record containing the byte content and suggested filename.
     */
    @Transactional(readOnly = true)
    public PdfFile generatePortfolioPdf(User user, String templateName, ExportVisibility visibility) {
        // The user object passed in is detached. We must fetch a fresh, fully-initialized
        // user within this transaction to avoid LazyInitializationException on collections like 'userSettings'.
        User freshUser = userService.findBySlugWithAllPortfolioData(user.getSlug())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with slug: " + user.getSlug()));

        // The templateName parameter is now ignored in favor of settings.
        // This ensures the user's chosen default is always respected for their portfolio.
        String effectiveTemplateName = freshUser.getUserSettings().stream()
                .filter(s -> "portfolio.pdf.template".equals(s.getName()))
                .map(UserSetting::getValue)
                .findFirst()
                .orElseGet(() -> settingRepository.findByName("DEFAULT_PDF_TEMPLATE")
                        .map(Setting::getValue)
                        .orElseGet(() -> settingRepository.findByName("portfolio.pdf.template")
                                .map(Setting::getValue)
                                .orElse("modern"))); // Final fallback if user and global settings are missing.

        // Phase 8 (8.2): backward compatibility — the retired "metro-dark"
        // key aliases to its print-safe replacement "metro-light" so existing
        // user preferences keep working instead of falling back to "modern".
        if ("metro-dark".equalsIgnoreCase(effectiveTemplateName)) {
            log.info("Legacy PDF template preference 'metro-dark' aliased to 'metro-light'.");
            effectiveTemplateName = "metro-light";
        }

        log.info("Starting PDF generation process for user '{}' with effective template: {}", freshUser.getSlug(), effectiveTemplateName);

        // Phase 8 (PRV-1): ALL content selection is delegated to the shared
        // ExportContentService - the requested visibility contract (PUBLIC =
        // exactly the public portfolio pages' filters; OWNER = the owner's
        // own hidden content) is enforced there, never here.
        PortfolioData portfolioData = exportContentService.buildExportData(freshUser, visibility);

        log.info("Generating PDF for user: {}", freshUser.getEmail());

        // 2. Select the template
        PortfolioPdfTemplate template = templates.get(effectiveTemplateName.toLowerCase());
        if (template == null) {
            log.error("Effective PDF template '{}' not found. Defaulting to 'modern'.", effectiveTemplateName);
            template = templates.get("modern");
        }

        // 3. Generate the PDF.
        // Phase 0.12 (OpenPDF migration): page size, margins and page events
        // are document-open-time concerns in OpenPDF, so the service applies
        // the template's declarations here and hands the template an OPEN
        // document (the previous engine allowed mid-render margin/event
        // changes).
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        float[] margins = template.getMargins();
        Rectangle pageSize = template.getPageSize();
        Document document = new Document(pageSize, margins[0], margins[1], margins[2], margins[3]);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            PdfPageEventHelper pageEvent = template.getPageEvent();
            if (pageEvent != null) {
                writer.setPageEvent(pageEvent);
            }
            document.open();
            PdfContext ctx = new PdfContext();
            template.generate(document, ctx, portfolioData);
        } catch (IOException | DocumentException e) {
            log.error("Failed to load fonts or other resources for PDF generation. Aborting.", e);
            throw new RuntimeException("Failed to generate PDF due to a resource loading issue.", e);
        } finally {
            document.close();
        }

        log.info("PDF generation complete. Final document size: {} bytes.", baos.size());
        String filename = String.format("%s%s-Resume-%s.pdf",
                freshUser.getFirstName(),
                freshUser.getLastName(),
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
        return new PdfFile(baos.toByteArray(), filename);
    }

    /**
     * The context object holds resources for a single PDF generation, like fonts.
     * It's public so it can be accessed by templates in other packages.
     *
     * <p>
     * Phase 0.12 (OpenPDF migration): fonts are carried as {@link BaseFont}
     * programs; templates compose sized/styled {@link Font} objects from
     * them at render time (OpenPDF has no font object that can be re-scaled
     * per use, unlike the previous engine's font handles).
     */
    public static class PdfContext {
        public final BaseFont nameFont, headlineFont, sectionHeaderFont, itemTitleFont, itemSubtitleFont, bodyFont, dateFont, solidIconFont, brandsIconFont;

        public PdfContext() throws IOException, DocumentException {
            this.nameFont = baseFont(BaseFont.HELVETICA_BOLD);
            this.headlineFont = baseFont(BaseFont.HELVETICA_OBLIQUE);
            this.sectionHeaderFont = baseFont(BaseFont.HELVETICA_BOLD);
            this.itemTitleFont = baseFont(BaseFont.HELVETICA_BOLD);
            this.itemSubtitleFont = baseFont(BaseFont.HELVETICA_OBLIQUE);
            this.bodyFont = baseFont(BaseFont.HELVETICA);
            this.dateFont = baseFont(BaseFont.HELVETICA);
            this.solidIconFont = loadOptionalFontFromResource("fonts/Font Awesome 6 Free-Solid-900.otf");
            this.brandsIconFont = loadOptionalFontFromResource("fonts/Font Awesome 6 Brands-Regular-400.otf");
        }

        private static BaseFont baseFont(String standardName) throws DocumentException, IOException {
            return BaseFont.createFont(standardName, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        }
    }

    public record PdfFile(byte[] content, String suggestedFilename) {
    }
}
