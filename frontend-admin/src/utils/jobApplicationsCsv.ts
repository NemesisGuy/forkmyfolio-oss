import type { JobApplication } from '@/types';
import { STATUS_META } from '@/types';
import { triggerDownload } from '@/utils/downloadUtils';

/**
 * Phase 0.32 (TASKS §I2): client-side CSV export of the job-application
 * list. The API returns the owner's full list (no server pagination), so
 * the export runs over whatever the list currently shows — i.e. it
 * respects the active search/filter/sort selection.
 *
 * RFC 4180: cells containing commas, quotes or line breaks are quoted,
 * embedded quotes are doubled, records are CRLF-terminated. A UTF-8 BOM
 * is prepended so Excel detects the encoding.
 */

/** Column order for the exported file (single source of truth + tests). */
export const CSV_HEADERS = [
  'Job Title',
  'Company',
  'Location',
  'Employment Type',
  'Work Arrangement',
  'Status',
  'Applied Date',
  'Deadline',
  'Follow-up',
  'Interview',
  'Assessment',
  'Salary Range',
  'Currency',
  'Period',
  'Job URL',
  'Resume Version',
  'Notes',
  'Created At',
] as const;

/** Quote a cell only when required by RFC 4180. */
const escapeCell = (value: unknown): string => {
  if (value === null || value === undefined) return '';
  const s = String(value);
  return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

/** One row per application, mirroring CSV_HEADERS. */
const toRow = (app: JobApplication): string => [
  app.jobTitle,
  app.companyName,
  app.jobLocation,
  app.jobType,
  app.workArrangement,
  STATUS_META[app.status]?.label ?? app.status,
  app.appliedAt,
  app.deadlineAt,
  app.followUpAt,
  app.interviewAt,
  app.assessmentAt,
  app.salaryRange,
  app.salaryCurrency,
  app.salaryPeriod,
  app.applicationUrl,
  app.resumeVersion,
  app.notes?.length ?? 0,
  app.createdAt,
].map(escapeCell).join(',');

/**
 * Build the full CSV document (BOM + header + one record per application).
 * Deterministic for the same input — no locale-dependent formatting.
 */
export const buildJobApplicationsCsv = (applications: JobApplication[]): string => {
  const records = applications.map(toRow);
  const lines = [CSV_HEADERS.join(','), ...records];
  // Trailing CRLF only when records exist (header-only output stays clean).
  return `\uFEFF${lines.join('\r\n')}${records.length ? '\r\n' : ''}`;
};

/** Build the CSV for `applications` and hand it to the browser download. */
export const downloadJobApplicationsCsv = (applications: JobApplication[]): void => {
  const csv = buildJobApplicationsCsv(applications);
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const today = new Date().toISOString().slice(0, 10);
  triggerDownload(blob, `job-applications-${today}.csv`);
};
