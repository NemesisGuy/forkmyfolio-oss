import { describe, it, expect } from 'vitest';
import { buildJobApplicationsCsv, CSV_HEADERS } from './jobApplicationsCsv';
import type { JobApplication } from '@/types';

/**
 * Phase 0.32 (TASKS §I2): CSV export contract — RFC 4180 escaping,
 * deterministic column order, UTF-8 BOM, status labels, empty-safe.
 */

const app = (overrides: Partial<JobApplication> & { uuid: string }): JobApplication => ({
  jobTitle: 'Developer',
  companyName: 'Acme',
  status: 'APPLIED',
  createdAt: '2026-01-01T00:00:00',
  ...overrides,
} as JobApplication);

/** Strip the BOM so assertions read the plain document. */
const body = (csv: string) => csv.replace(/^\uFEFF/, '');

describe('jobApplicationsCsv (Phase 0.32)', () => {
  it('starts with a UTF-8 BOM and the canonical header row', () => {
    const csv = buildJobApplicationsCsv([]);
    expect(csv.charCodeAt(0)).toBe(0xfeff);
    expect(body(csv).split('\r\n')[0]).toBe(CSV_HEADERS.join(','));
  });

  it('emits header-only output for an empty list (no dangling record separator)', () => {
    const csv = buildJobApplicationsCsv([]);
    expect(body(csv)).toBe(CSV_HEADERS.join(','));
    expect(csv.endsWith('\r\n')).toBe(false);
  });

  it('writes one record per application, in input order', () => {
    const csv = buildJobApplicationsCsv([
      app({ uuid: 'a1', jobTitle: 'Backend Engineer', companyName: 'Alpha' }),
      app({ uuid: 'a2', jobTitle: 'Frontend Engineer', companyName: 'Beta' }),
    ]);
    const lines = body(csv).split('\r\n').filter(Boolean);
    expect(lines).toHaveLength(3); // header + 2 records
    expect(lines[1]).toContain('Backend Engineer,Alpha');
    expect(lines[2]).toContain('Frontend Engineer,Beta');
  });

  it('quotes cells containing commas, doubles embedded quotes, wraps newlines', () => {
    const csv = buildJobApplicationsCsv([
      app({ uuid: 'a1', jobTitle: 'Engineer, Senior', companyName: 'The "Best" Co' }),
      app({ uuid: 'a2', jobTitle: 'Line1\nLine2' }),
    ]);
    const lines = body(csv).split('\r\n');
    // Row 1 stays a single record despite the embedded comma + quotes...
    expect(lines[1]).toContain('"Engineer, Senior"');
    expect(lines[1]).toContain('"The ""Best"" Co"');
    // ...and row 2's embedded newline survives inside one logical record.
    expect(lines[2]).toContain('"Line1\nLine2"');
    expect(lines[2]).toContain(',Acme,,,,Applied,'); // company then 3 empty cells then status
  });

  it('maps statuses to human labels (incl. legacy markers)', () => {
    const csv = buildJobApplicationsCsv([
      app({ uuid: 'a1', status: 'APPLIED' }),
      app({ uuid: 'a2', status: 'INTERVIEWING' }),
    ]);
    const rows = body(csv).split('\r\n').filter(Boolean);
    expect(rows[1]).toContain(',Applied,');
    expect(rows[2]).toContain(',Interviewing (legacy),');
  });

  it('renders missing optional fields as empty cells and notes as a count', () => {
    const csv = buildJobApplicationsCsv([
      app({ uuid: 'a1', jobLocation: undefined, salaryRange: undefined, notes: undefined }),
      app({ uuid: 'a2', notes: [{ uuid: 'n1', content: 'x', createdAt: '2026-01-02T00:00:00' }] }),
    ]);
    const rows = body(csv).split('\r\n').filter(Boolean);
    // 18 columns => 17 separators per row
    expect(rows[1].split(',').length).toBeGreaterThanOrEqual(17);
    expect(rows[2]).toMatch(/,1,\d{4}-/); // notes count column
  });

  it('is deterministic: same input produces byte-identical output', () => {
    const input = [app({ uuid: 'a1', jobTitle: 'X, Y' }), app({ uuid: 'a2', status: 'OFFER' })];
    expect(buildJobApplicationsCsv(input)).toBe(buildJobApplicationsCsv(input));
  });
});
