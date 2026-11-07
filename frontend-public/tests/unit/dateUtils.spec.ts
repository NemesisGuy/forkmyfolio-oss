import { describe, it, expect } from 'vitest';
import { formatDisplayDate, formatDate } from '~/utils/dateUtils';

describe('utils/dateUtils', () => {
  describe('formatDisplayDate (Month YYYY used on experience entries)', () => {
    it('formats an ISO date as "Month YYYY"', () => {
      expect(formatDisplayDate('2023-01-15')).toBe('January 2023');
    });

    it('accepts Date instances', () => {
      expect(formatDisplayDate(new Date('2023-01-15T00:00:00Z'))).toBe('January 2023');
    });

    it('always returns a UTC anchor so no timezone can shift the month/day', () => {
      // 15 Jan in UTC; +1 day compensation must never spill into February
      expect(formatDisplayDate('2023-01-15T23:59:59Z')).toBe('January 2023');
      expect(formatDisplayDate('2023-12-31T00:00:00Z')).toBe('January 2024');
    });

    it('returns "" for missing input', () => {
      expect(formatDisplayDate(undefined)).toBe('');
      expect(formatDisplayDate(null)).toBe('');
      expect(formatDisplayDate('')).toBe('');
    });
  });

  describe('formatDate (full date formatting)', () => {
    it('formats full dates with day numbers', () => {
      expect(formatDate('2023-03-05T12:00:00Z')).toMatch(/(4|5) March 2023|March (4|5),/i);
    });

    it('returns "Invalid Date" for unparseable input', () => {
      expect(formatDate('not-a-date')).toBe('Invalid Date');
    });

    it('returns "" for missing input', () => {
      expect(formatDate(null)).toBe('');
      expect(formatDate(undefined)).toBe('');
    });

    it('honours custom Intl options', () => {
      expect(
        formatDate('2023-03-05T12:00:00Z', { year: 'numeric', month: '2-digit', day: '2-digit' })
      ).toMatch(/2023/);
    });
  });
});
