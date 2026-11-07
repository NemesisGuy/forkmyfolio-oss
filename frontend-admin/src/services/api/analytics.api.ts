import { fetchWithAuth } from './apiClient';

/**
 * @file src/services/api/analytics.api.ts
 * @description API functions for the analytics feature (DEF-1): provider
 * configuration (admin + per-owner) and reporting proxies. No function here
 * ever receives or returns a provider credential — the API key is write-only
 * and lives encrypted server-side.
 */

// --- Types ---

export type AnalyticsProviderType = 'NONE' | 'UMAMI' | 'GA4';
export type AnalyticsOwnerMode = 'INHERIT' | 'OWN' | 'DISABLED';

export interface AnalyticsSummary {
  pageviews: number;
  visitors: number;
  visits: number;
  bounces: number;
  totalTimeSeconds: number;
  comparison?: {
    pageviews: number;
    visitors: number;
    visits: number;
    bounces: number;
    totalTimeSeconds: number;
  };
}

export interface AnalyticsTimeSeriesPoint {
  timestamp: number;
  value: number;
}

export interface AnalyticsTimeSeries {
  pageviews: AnalyticsTimeSeriesPoint[];
  sessions: AnalyticsTimeSeriesPoint[];
}

export interface AnalyticsMetricRow {
  name: string;
  value: number;
}

export interface AnalyticsConnectionTest {
  success: boolean;
  message: string;
  providerStatus?: number | null;
}

export interface AnalyticsConfig {
  uuid: string;
  provider: AnalyticsProviderType;
  trackingEnabled: boolean;
  reportingEnabled: boolean;
  recorderEnabled: boolean;
  umamiBaseUrl: string | null;
  umamiWebsiteName: string | null;
  umamiWebsiteId: string | null;
  umamiScriptPath: string;
  umamiRecorderPath: string;
  ga4MeasurementId: string | null;
  apiKeyLast4: string | null;
  apiKeyConfigured: boolean;
  credentialStorageAvailable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateAnalyticsConfig {
  provider?: AnalyticsProviderType;
  trackingEnabled?: boolean;
  reportingEnabled?: boolean;
  recorderEnabled?: boolean;
  umamiBaseUrl?: string;
  umamiWebsiteName?: string;
  umamiWebsiteId?: string;
  umamiScriptPath?: string;
  umamiRecorderPath?: string;
  ga4MeasurementId?: string;
  /** Write-only: null/undefined keeps the stored key, '' clears it, a value replaces it. */
  umamiApiKey?: string;
}

export interface UserAnalyticsConfig {
  uuid: string;
  mode: AnalyticsOwnerMode;
  provider: AnalyticsProviderType;
  trackingEnabled: boolean;
  reportingEnabled: boolean;
  recorderEnabled: boolean;
  umamiBaseUrl: string | null;
  umamiWebsiteName: string | null;
  umamiWebsiteId: string | null;
  umamiScriptPath: string;
  umamiRecorderPath: string;
  ga4MeasurementId: string | null;
  apiKeyLast4: string | null;
  apiKeyConfigured: boolean;
  effectiveProvider: AnalyticsProviderType;
  effectiveTrackingEnabled: boolean;
  effectiveReportingEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserAnalyticsConfig {
  mode?: AnalyticsOwnerMode;
  provider?: AnalyticsProviderType;
  trackingEnabled?: boolean;
  reportingEnabled?: boolean;
  recorderEnabled?: boolean;
  umamiBaseUrl?: string;
  umamiWebsiteName?: string;
  umamiWebsiteId?: string;
  umamiScriptPath?: string;
  umamiRecorderPath?: string;
  ga4MeasurementId?: string;
  /** Write-only: null/undefined keeps the stored key, '' clears it, a value replaces it. */
  umamiApiKey?: string;
}

export type AnalyticsMetricType = 'PATH' | 'REFERRER' | 'COUNTRY' | 'BROWSER' | 'OS' | 'DEVICE' | 'EVENT' | 'TITLE';

export interface AnalyticsRangeParams {
  /** Whitelisted relative period: 24h | 7d | 30d | 90d | 12month */
  period?: string;
  startAt?: number;
  endAt?: number;
}

// --- Helpers ---

const toQuery = (params: AnalyticsRangeParams = {}): string => {
  const search = new URLSearchParams();
  if (params.period) search.set('period', params.period);
  if (params.startAt !== undefined) search.set('startAt', String(params.startAt));
  if (params.endAt !== undefined) search.set('endAt', String(params.endAt));
  const qs = search.toString();
  return qs ? `?${qs}` : '';
};

// --- Admin: platform configuration ---

export const getAnalyticsConfig = (): Promise<AnalyticsConfig> =>
  fetchWithAuth<AnalyticsConfig>('/admin/analytics/config', { method: 'GET' });

export const updateAnalyticsConfig = (update: UpdateAnalyticsConfig): Promise<AnalyticsConfig> =>
  fetchWithAuth<AnalyticsConfig>('/admin/analytics/config', { method: 'PUT', body: update });

export const testAnalyticsConnection = (): Promise<AnalyticsConnectionTest> =>
  fetchWithAuth<AnalyticsConnectionTest>('/admin/analytics/config/test', { method: 'POST' });

// --- Admin: master (site-wide) reporting ---

export const getAdminAnalyticsSummary = (params?: AnalyticsRangeParams): Promise<AnalyticsSummary> =>
  fetchWithAuth<AnalyticsSummary>(`/admin/analytics/summary${toQuery(params)}`, { method: 'GET' });

export const getAdminAnalyticsTimeseries = (params?: AnalyticsRangeParams): Promise<AnalyticsTimeSeries> =>
  fetchWithAuth<AnalyticsTimeSeries>(`/admin/analytics/timeseries${toQuery(params)}`, { method: 'GET' });

export const getAdminAnalyticsMetrics = (
  type: AnalyticsMetricType,
  params?: AnalyticsRangeParams,
  limit = 10
): Promise<AnalyticsMetricRow[]> =>
  fetchWithAuth<AnalyticsMetricRow[]>(
    `/admin/analytics/metrics?type=${type}&limit=${limit}${toQuery(params).replace('?', '&')}`,
    { method: 'GET' }
  );

// --- Owner: my configuration ---

export const getMyAnalyticsConfig = (): Promise<UserAnalyticsConfig> =>
  fetchWithAuth<UserAnalyticsConfig>('/me/analytics/config', { method: 'GET' });

export const updateMyAnalyticsConfig = (update: UpdateUserAnalyticsConfig): Promise<UserAnalyticsConfig> =>
  fetchWithAuth<UserAnalyticsConfig>('/me/analytics/config', { method: 'PUT', body: update });

export const testMyAnalyticsConnection = (): Promise<AnalyticsConnectionTest> =>
  fetchWithAuth<AnalyticsConnectionTest>('/me/analytics/config/test', { method: 'POST' });

// --- Owner: my portfolio reporting ---

export const getMyAnalyticsSummary = (params?: AnalyticsRangeParams): Promise<AnalyticsSummary> =>
  fetchWithAuth<AnalyticsSummary>(`/me/analytics/summary${toQuery(params)}`, { method: 'GET' });

export const getMyAnalyticsTimeseries = (params?: AnalyticsRangeParams): Promise<AnalyticsTimeSeries> =>
  fetchWithAuth<AnalyticsTimeSeries>(`/me/analytics/timeseries${toQuery(params)}`, { method: 'GET' });

export const getMyAnalyticsMetrics = (
  type: AnalyticsMetricType,
  params?: AnalyticsRangeParams,
  limit = 10
): Promise<AnalyticsMetricRow[]> =>
  fetchWithAuth<AnalyticsMetricRow[]>(
    `/me/analytics/metrics?type=${type}&limit=${limit}${toQuery(params).replace('?', '&')}`,
    { method: 'GET' }
  );
