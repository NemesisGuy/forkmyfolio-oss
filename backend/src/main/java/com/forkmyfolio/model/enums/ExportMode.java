package com.forkmyfolio.model.enums;

/**
 * Phase 8.3: the export mode an owner configures for their OWN downloads
 * (PDF and Markdown). Persisted as the {@code export.mode} UserSetting
 * override; PUBLIC exports never consult it.
 *
 * <ul>
 *   <li>{@link #FULL} — the complete portfolio: every enabled section in
 *       its full supported form (the historical behaviour, and the safe
 *       default for users who have never configured export preferences).</li>
 *   <li>{@link #BRIEF} — a genuinely concise CV: enabled sections render
 *     without long-form descriptions and with bullet lists capped, reusing
 *     existing data only (nothing is invented or rewritten).</li>
 * </ul>
 */
public enum ExportMode {

    /** Complete portfolio — all enabled sections in full form (default). */
    FULL,

    /** Concise resume — long-form descriptions omitted, bullets capped. */
    BRIEF
}
