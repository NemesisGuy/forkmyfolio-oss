package com.forkmyfolio.service.pdf.templates;

import com.forkmyfolio.service.impl.PdfGenerationService;
import com.forkmyfolio.service.pdf.PortfolioData;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPageEventHelper;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

/**
 * Phase 8 (8.2): "Metro Light" — the print-safe replacement for the retired
 * Metro Dark template. Keeps the Metro typographic identity (Montserrat
 * headings, Lato body, big upper-case name, vibrant blue accent, thin accent
 * rules) but renders on a WHITE canvas: no canvas-wide background fill, so
 * the document prints and previews identically in every viewer (the defect
 * that retired Metro Dark).
 *
 * <p>
 * All layout blocks come from the shared {@link TemplateSupport} library;
 * this class only defines the theme and the block order.
 *
 * <p>
 * Phase 0.12 (OpenPDF migration): fonts are loaded once in the constructor
 * as {@link BaseFont} programs (the previous engine re-loaded them per
 * render); margins and the page-number event are declared on the interface
 * and applied by the service before the document opens.
 */
public class MetroLightTemplate implements PortfolioPdfTemplate {

    // Metro identity palette, re-tuned for a light canvas
    private static final Color TEXT_COLOR = new Color(25, 25, 30);      // near-black
    private static final Color SUBTLE_COLOR = new Color(110, 110, 118); // medium grey
    private static final Color ACCENT_COLOR = new Color(0, 150, 255);   // #0096FF vibrant blue

    private final BaseFont headingFont;
    private final BaseFont bodyFont;
    private final BaseFont bodyItalicFont;

    public MetroLightTemplate() throws IOException, DocumentException {
        this.headingFont = loadFontFromResource("fonts/Montserrat-Bold.ttf");
        this.bodyFont = loadFontFromResource("fonts/Lato-Regular.ttf");
        this.bodyItalicFont = loadFontFromResource("fonts/Lato-Italic.ttf");
    }

    @Override
    public float[] getMargins() {
        // Previous engine: setMargins(40, 40, 40, 40)
        return new float[]{40, 40, 40, 40}; // {left, right, top, bottom}
    }

    @Override
    public PdfPageEventHelper getPageEvent() {
        return TemplateSupport.pageNumberEvent(bodyFont, SUBTLE_COLOR);
    }

    @Override
    public void generate(Document document, PdfGenerationService.PdfContext ctx, PortfolioData data)
            throws IOException, DocumentException {
        TemplateSupport.Theme theme = new TemplateSupport.Theme(
                headingFont, bodyItalicFont, headingFont, headingFont, bodyItalicFont, bodyFont, bodyFont,
                TEXT_COLOR, SUBTLE_COLOR, ACCENT_COLOR,
                true,   // upper-case headings — Metro signature
                1.3f);  // generous leading

        TemplateSupport.renderHeader(document, theme, data);
        TemplateSupport.renderProfileImage(document, data, 90);
        TemplateSupport.renderSummary(document, theme, data);
        TemplateSupport.renderExperienceSection(document, theme, data);
        TemplateSupport.renderProjectsSection(document, theme, data);
        TemplateSupport.renderSkillsSection(document, theme, data);
        TemplateSupport.renderQualificationsSection(document, theme, data);
        TemplateSupport.renderTestimonialsSection(document, theme, data);
        TemplateSupport.renderFooter(document, theme);
    }

    private static BaseFont loadFontFromResource(String resourcePath) throws IOException, DocumentException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Font resource not found: " + resourcePath);
            }
            // The resource path doubles as the OpenPDF font cache key; its
            // .ttf extension selects the TrueType parser. Embedded, WinAnsi
            // encoded — matching the previous engine's default encoding.
            return BaseFont.createFont(resourcePath, BaseFont.WINANSI, BaseFont.EMBEDDED, true, is.readAllBytes(), null);
        }
    }
}
