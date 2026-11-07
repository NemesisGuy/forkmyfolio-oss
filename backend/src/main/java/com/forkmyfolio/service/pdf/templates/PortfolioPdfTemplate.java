package com.forkmyfolio.service.pdf.templates;

import com.forkmyfolio.model.Experience;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.Skill;
import com.forkmyfolio.service.impl.PdfGenerationService;
import com.forkmyfolio.service.pdf.PortfolioData;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPageEventHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Defines the contract for a PDF template. Each implementation is responsible
 * for rendering the portfolio data into the provided OpenPDF Document.
 *
 * <p>
 * Phase 0.12 (OpenPDF migration): the document handed to
 * {@link #generate} is already open, with the page size, margins and page
 * events (e.g. page numbering) applied by {@link PdfGenerationService} from
 * the {@link #getPageSize()}, {@link #getMargins()} and
 * {@link #getPageEvent()} declarations — OpenPDF fixes margins and page
 * events at document-open time, unlike the previous layout engine.
 */
public interface PortfolioPdfTemplate {

    /**
     * Generates the full PDF content and layout into the document.
     *
     * @param document The OpenPDF Document to which content will be added (already open).
     * @param context  The context containing shared resources like fonts.
     * @param data     The portfolio data to be rendered.
     * @throws IOException        if a resource (like a font) cannot be loaded.
     * @throws DocumentException  if the OpenPDF document rejects an element.
     */
    void generate(Document document, PdfGenerationService.PdfContext context, PortfolioData data)
            throws IOException, DocumentException;

    /**
     * Phase 7.4 (§8): single source of truth for grouping skills into
     * category sections for rendering, mirroring the Markdown export
     * (categories in encounter order; uncategorised skills pooled under
     * "Other Skills"). Used by every template that renders a Skills section.
     *
     * <p>
     * Phase 8 (8.2): the implementation moved to the shared
     * {@link TemplateSupport} block library so legacy and new templates
     * share exactly one implementation.
     *
     * @param skills the visible portfolio skills.
     * @return category -> skills map in render order (never null).
     */
    default Map<String, List<Skill>> skillsGroupedByCategory(List<Skill> skills) {
        return TemplateSupport.groupSkillsByCategory(skills);
    }

    /**
     * Phase 7.4 (§6): single source of truth for a project's structured
     * key-feature lines in a stable display order. Templates render these
     * bullets (or nothing when the project uses the legacy embedded-Markdown
     * convention) after the project description.
     *
     * <p>
     * Phase 8 (8.2): implementation moved to {@link TemplateSupport}.
     *
     * @param project the project whose key features should be rendered.
     * @return the key-feature values in display order, or an empty list.
     */
    default List<String> keyFeatureLines(Project project) {
        return TemplateSupport.keyFeatureValues(project);
    }

    /**
     * Phase 7.4 (§7): single source of truth for splitting an experience's
     * achievements into displayable lines. The Markdown export has rendered
     * one bullet per line since its introduction; the PDF templates
     * previously OMITTED achievements entirely (parity defect). Templates
     * must render these lines (or nothing when blank) after the experience
     * description.
     *
     * <p>
     * Phase 8 (8.2): implementation moved to {@link TemplateSupport}.
     *
     * @param experience the experience whose achievements should be rendered.
     * @return the trimmed, non-blank achievement lines, or an empty list.
     */
    default List<String> achievementLines(Experience experience) {
        return TemplateSupport.achievementValues(experience);
    }

    /**
     * Defines the page size for this template.
     *
     * @return The page size rectangle. Defaults to A4.
     */
    default Rectangle getPageSize() {
        return org.openpdf.text.PageSize.A4;
    }

    /**
     * Phase 0.12 (OpenPDF migration): the page margins as
     * {@code {left, right, top, bottom}} in points, applied when the service
     * constructs the document. Defaults to 0 on all sides — the previous
     * engine constructed every document with zero margins and each template
     * overrode them for its own layout; the "modern" template is the only
     * one that keeps the zero-margin default (full-bleed banner and sidebar).
     */
    default float[] getMargins() {
        return new float[]{0, 0, 0, 0};
    }

    /**
     * Phase 0.12 (OpenPDF migration): an optional page event (page
     * numbering, background fills) registered on the writer before the
     * document opens. OpenPDF page events must be attached before
     * {@code Document.open()}, so templates declare theirs here instead of
     * registering mid-render.
     */
    default PdfPageEventHelper getPageEvent() {
        return null;
    }
}
