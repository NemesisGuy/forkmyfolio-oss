import { describe, it, expect } from 'vitest';
import {
  canShowCoverLetterButton,
  isPublicCoverLetterEnabled,
  PUBLIC_COVER_LETTER_SETTING,
} from '#shared/utils/coverLetter';

/**
 * Phase 0.28 (B1): the public "View Cover Letter" button gating.
 *
 * Contract:
 * - the owner's `export.coverletter.public` switch is the single control;
 * - fail closed — only the literal `IN_PDF` enables visitor exposure
 *   (mirrors the backend's public cover-letter semantics);
 * - a letter without the switch, or a switch without a letter, never shows.
 */
describe('shared/utils/coverLetter', () => {
  describe('isPublicCoverLetterEnabled', () => {
    it('is enabled only for the exact value IN_PDF', () => {
      expect(isPublicCoverLetterEnabled({ [PUBLIC_COVER_LETTER_SETTING]: 'IN_PDF' })).toBe(true);
      expect(isPublicCoverLetterEnabled({ [PUBLIC_COVER_LETTER_SETTING]: 'OFF' })).toBe(false);
      // SEPARATE is an OWNER-only mode: it must fail closed for visitors,
      // exactly like the backend's standalone endpoint 404s for guests.
      expect(isPublicCoverLetterEnabled({ [PUBLIC_COVER_LETTER_SETTING]: 'SEPARATE' })).toBe(false);
      expect(isPublicCoverLetterEnabled({ [PUBLIC_COVER_LETTER_SETTING]: 'garbage' })).toBe(false);
      expect(isPublicCoverLetterEnabled({ [PUBLIC_COVER_LETTER_SETTING]: ' in_pdf ' })).toBe(true);
    });

    it('fails closed when the setting is missing or the map is empty', () => {
      expect(isPublicCoverLetterEnabled({})).toBe(false);
      expect(isPublicCoverLetterEnabled({ other: 'IN_PDF' })).toBe(false);
      expect(isPublicCoverLetterEnabled(null)).toBe(false);
      expect(isPublicCoverLetterEnabled(undefined)).toBe(false);
    });
  });

  describe('canShowCoverLetterButton', () => {
    const letter = 'Dear Hiring Manager,\n\nI am applying for the role.';

    it('shows the button only with BOTH letter text and the switch on', () => {
      expect(canShowCoverLetterButton(letter, { [PUBLIC_COVER_LETTER_SETTING]: 'IN_PDF' })).toBe(true);
    });

    it('hides the button when the owner disabled the cover letter', () => {
      expect(canShowCoverLetterButton(letter, { [PUBLIC_COVER_LETTER_SETTING]: 'OFF' })).toBe(false);
      expect(canShowCoverLetterButton(letter, {})).toBe(false);
      expect(canShowCoverLetterButton(letter, null)).toBe(false);
    });

    it('hides the button when there is no letter text (blank counts as none)', () => {
      const on = { [PUBLIC_COVER_LETTER_SETTING]: 'IN_PDF' };
      expect(canShowCoverLetterButton('', on)).toBe(false);
      expect(canShowCoverLetterButton('   \n  ', on)).toBe(false);
      expect(canShowCoverLetterButton(null, on)).toBe(false);
      expect(canShowCoverLetterButton(undefined, on)).toBe(false);
    });
  });
});
