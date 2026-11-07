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
 * Phase 8 (8.2): "Executive" — a restrained, board-room template: serif
 * typography, muted navy accents, thin rules, generous leading, NO background
 * fills (print-safe by construction).
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
public class ExecutiveTemplate implements PortfolioPdfTemplate {

    private static final Color TEXT_COLOR = new Color(30, 34, 44);      // ink
    private static final Color SUBTLE_COLOR = new Color(105, 110, 122); // cool grey
    private static final Color ACCENT_COLOR = new Color(28, 52, 91);    // muted navy

    private final BaseFont serifBold;
    private final BaseFont serifRegular;
    private final BaseFont serifItalic;

    public ExecutiveTemplate() throws IOException, DocumentException {
        this.serifBold = BaseFont.createFont(BaseFont.TIMES_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        this.serifRegular = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        this.serifItalic = BaseFont.createFont(BaseFont.TIMES_ITALIC, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    @Override
    public float[] getMargins() {
        // Previous engine: setMargins(top=50, right=55, bottom=45, left=55)
        return new float[]{55, 55, 50, 45}; // {left, right, top, bottom}
    }

    @Override
    public PdfPageEventHelper getPageEvent() {
        return TemplateSupport.pageNumberEvent(serifRegular, SUBTLE_COLOR);
    }

    @Override
    public void generate(Document document, PdfGenerationService.PdfContext ctx, PortfolioData data)
            throws IOException, DocumentException {
        TemplateSupport.Theme theme = new TemplateSupport.Theme(
                serifBold, serifItalic, serifBold, serifBold, serifItalic, serifRegular, serifRegular,
                TEXT_COLOR, SUBTLE_COLOR, ACCENT_COLOR,
                false,  // mixed-case headings — Executive restraint
                1.5f);  // generous leading

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
