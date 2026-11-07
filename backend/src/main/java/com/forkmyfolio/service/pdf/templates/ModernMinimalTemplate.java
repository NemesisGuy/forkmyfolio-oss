package com.forkmyfolio.service.pdf.templates;

import com.forkmyfolio.service.impl.PdfGenerationService;
import com.forkmyfolio.service.pdf.PortfolioData;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPageEventHelper;

import java.awt.Color;
import java.io.IOException;

/**
 * Phase 8 (8.2): "Modern Minimal" — whitespace-driven single-column layout:
 * small-caps section labels, thin divider lines, tight typographic palette,
 * NO background fills (print-safe by construction).
 *
 * <p>
 * All layout blocks come from the shared {@link TemplateSupport} library;
 * this class only defines the theme and the block order.
 *
 * <p>
 * Phase 0.12 (OpenPDF migration): fonts are {@link BaseFont} programs;
 * margins and the page-number event are declared on the interface and
 * applied by the service before the document opens.
 */
public class ModernMinimalTemplate implements PortfolioPdfTemplate {

    private static final Color TEXT_COLOR = new Color(38, 38, 42);      // soft black
    private static final Color SUBTLE_COLOR = new Color(130, 130, 136); // quiet grey
    private static final Color ACCENT_COLOR = new Color(17, 24, 39);    // near-black accent (monochrome identity)

    private final BaseFont helveticaBold;
    private final BaseFont helvetica;
    private final BaseFont helveticaOblique;

    public ModernMinimalTemplate() throws IOException, DocumentException {
        this.helveticaBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        this.helvetica = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        this.helveticaOblique = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    @Override
    public float[] getMargins() {
        // Previous engine: setMargins(top=55, right=60, bottom=45, left=60)
        return new float[]{60, 60, 55, 45}; // {left, right, top, bottom}
    }

    @Override
    public PdfPageEventHelper getPageEvent() {
        return TemplateSupport.pageNumberEvent(helvetica, SUBTLE_COLOR);
    }

    @Override
    public void generate(Document document, PdfGenerationService.PdfContext ctx, PortfolioData data)
            throws IOException, DocumentException {
        TemplateSupport.Theme theme = new TemplateSupport.Theme(
                helveticaBold, helveticaOblique, helveticaBold, helveticaBold, helveticaOblique, helvetica, helvetica,
                TEXT_COLOR, SUBTLE_COLOR, ACCENT_COLOR,
                true,   // upper-case (small-caps feel via compact sizing)
                1.45f); // airy leading

        TemplateSupport.renderHeader(document, theme, data);
        TemplateSupport.renderSummary(document, theme, data);
        TemplateSupport.renderExperienceSection(document, theme, data);
        TemplateSupport.renderProjectsSection(document, theme, data);
        TemplateSupport.renderSkillsSection(document, theme, data);
        TemplateSupport.renderQualificationsSection(document, theme, data);
        TemplateSupport.renderTestimonialsSection(document, theme, data);
        TemplateSupport.renderFooter(document, theme);
    }
}
