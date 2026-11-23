/**
 * @file shared/utils/coverLetter.ts
 * @description Phase 0.28 (B1): single source of truth for whether a visitor
 * may see the owner's cover letter.
 *
 * The owner's `export.coverletter.public` switch is the one control that
 * governs PUBLIC cover-letter exposure (seeded OFF). The backend mirrors the
 * same fail-closed rule: only the literal value `IN_PDF` lets visitors
 * receive the letter — anything else (OFF, SEPARATE, garbage, missing)
 * means visitors get the resume only.
 *
 * Used by the public portfolio page to gate the "View Cover Letter" button
 * (and therefore its modal) so a disabled cover letter is never advertised.
 */

/** Setting name of the owner's public cover-letter switch. */
export const PUBLIC_COVER_LETTER_SETTING = 'export.coverletter.public';

/**
 * Whether the owner enabled their cover letter for visitors.
 *
 * @param settings Normalized `{name: value}` settings map (may be missing
 *   while loading — treated as disabled, i.e. fail closed).
 * @returns `true` only for the exact value `IN_PDF`.
 */
export function isPublicCoverLetterEnabled(
  settings: Record<string, string> | null | undefined
): boolean {
  const raw = settings?.[PUBLIC_COVER_LETTER_SETTING];
  if (!raw) return false;
  return raw.trim().toUpperCase() === 'IN_PDF';
}

/**
 * Whether the public "View Cover Letter" button should render.
 *
 * Requires BOTH the owner's letter text AND the public switch being on —
 * a letter with the switch off is private to the owner's own exports.
 *
 * @param letterText The profile's raw cover-letter text.
 * @param settings Normalized settings map for the portfolio's slug.
 */
export function canShowCoverLetterButton(
  letterText: string | null | undefined,
  settings: Record<string, string> | null | undefined
): boolean {
  if (!letterText || !letterText.trim()) return false;
  return isPublicCoverLetterEnabled(settings);
}
