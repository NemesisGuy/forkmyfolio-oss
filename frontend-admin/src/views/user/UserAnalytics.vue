<template>
  <LoadingModal :visible="isLoading || isSaving"/>

  <div class="user-analytics-page py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-lg-10 col-xl-9">
          <h1 class="display-5 mb-4">Analytics</h1>
          <p class="lead text-muted mb-2">
            Visitor analytics for your public portfolio.
          </p>
          <p class="text-muted small mb-5">
            <Info class="me-1" aria-hidden="true"/>
            By default your portfolio is measured under the platform's analytics website
            (<strong>inherit</strong>). You can plug in your own Umami instance instead, or turn
            analytics off for your portfolio entirely. Credentials are stored encrypted and are
            never exposed to browsers.
          </p>

          <!-- Error State -->
          <div v-if="loadError" class="alert alert-danger">
            <h4 class="alert-heading"><Ban class="me-1" aria-hidden="true"/> Error</h4>
            <p>Could not load your analytics settings. Please try again later.</p>
          </div>

          <template v-else>
            <!-- Configuration -->
            <form @submit.prevent="handleSaveConfig">
              <div class="card glass-card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                  <h5 class="mb-0">Configuration</h5>
                  <span :class="modeBadgeClass">{{ modeBadgeText }}</span>
                </div>
                <div class="card-body p-4">
                  <div class="form-check mb-3">
                    <input id="mode-inherit" v-model="draft.mode" class="form-check-input" name="analyticsMode"
                           type="radio" value="INHERIT" :disabled="isSaving" @change="isDirty = true">
                    <label class="form-check-label" for="mode-inherit">
                      <strong>Inherit platform analytics</strong>
                      <p class="form-text text-muted mt-0">
                        Your portfolio's traffic is measured under the platform's analytics website.
                        Nothing for you to configure.
                      </p>
                    </label>
                  </div>
                  <div class="form-check mb-3">
                    <input id="mode-own" v-model="draft.mode" class="form-check-input" name="analyticsMode"
                           type="radio" value="OWN" :disabled="isSaving" @change="isDirty = true">
                    <label class="form-check-label" for="mode-own">
                      <strong>Use my own Umami instance</strong>
                      <p class="form-text text-muted mt-0">
                        Point at your own Umami website. Your data stays on your instance and only
                        you can query it.
                      </p>
                    </label>
                  </div>
                  <div class="form-check">
                    <input id="mode-disabled" v-model="draft.mode" class="form-check-input" name="analyticsMode"
                           type="radio" value="DISABLED" :disabled="isSaving" @change="isDirty = true">
                    <label class="form-check-label" for="mode-disabled">
                      <strong>Disable analytics for my portfolio</strong>
                      <p class="form-text text-muted mt-0">
                        No tracking script is loaded on your public pages and no dashboard is available.
                      </p>
                    </label>
                  </div>

                  <!-- Own-instance fields -->
                  <div v-if="draft.mode === 'OWN'" class="mt-4 pt-3 border-top">
                    <div class="row g-3">
                      <div class="col-md-4">
                        <label class="form-label" for="ownProvider">Provider</label>
                        <select id="ownProvider" v-model="draft.provider" class="form-select"
                                :disabled="isSaving" @change="isDirty = true">
                          <option value="UMAMI">Umami</option>
                          <option value="GA4">Google Analytics 4</option>
                        </select>
                      </div>

                      <!-- Umami-specific fields -->
                      <template v-if="draft.provider === 'UMAMI'">
                        <div class="col-md-8">
                          <label class="form-label" for="ownBaseUrl">Umami Base URL</label>
                          <input id="ownBaseUrl" v-model="draft.umamiBaseUrl" class="form-control" type="url"
                                 placeholder="https://metrics.example.com" :disabled="isSaving" @input="isDirty = true">
                          <div class="form-text text-muted">Root URL of your Umami instance (https required).</div>
                        </div>
                        <div class="col-md-4">
                          <label class="form-label" for="ownWebsiteName">Website Name <span class="text-muted small">(optional)</span></label>
                          <input id="ownWebsiteName" v-model="draft.umamiWebsiteName" class="form-control" type="text"
                                 placeholder="e.g. My Portfolio" :disabled="isSaving" @input="isDirty = true">
                          <div class="form-text text-muted">Display label only.</div>
                        </div>
                        <div class="col-md-4">
                          <label class="form-label" for="ownWebsiteId">Website ID</label>
                          <input id="ownWebsiteId" v-model="draft.umamiWebsiteId" class="form-control" type="text"
                                 placeholder="e.g. 54483748-3843-..." :disabled="isSaving" @input="isDirty = true">
                          <div class="form-text text-muted">The public UUID of your website in Umami.</div>
                        </div>
                        <div class="col-md-4">
                          <label class="form-label" for="ownScriptPath">Tracking Script Path</label>
                          <input id="ownScriptPath" v-model="draft.umamiScriptPath" class="form-control" type="text"
                                 placeholder="/script.js" :disabled="isSaving" @input="isDirty = true">
                          <div class="form-text text-muted">Default <code>/script.js</code>.</div>
                        </div>
                        <div class="col-md-4">
                          <label class="form-label" for="ownRecorderPath">Recorder Script Path</label>
                          <input id="ownRecorderPath" v-model="draft.umamiRecorderPath" class="form-control" type="text"
                                 placeholder="/recorder.js" :disabled="isSaving" @input="isDirty = true">
                          <div class="form-text text-muted">Default <code>/recorder.js</code>.</div>
                        </div>
                        <div class="col-md-6">
                          <label class="form-label" for="ownApiKey">
                            Umami API Key
                            <span class="badge ms-1 align-middle"
                                  :class="config?.apiKeyConfigured ? 'bg-success' : 'bg-secondary'">
                              {{ config?.apiKeyConfigured ? `stored ••••${config.apiKeyLast4}` : 'not stored' }}
                            </span>
                          </label>
                          <div class="d-flex gap-2">
                            <input id="ownApiKey" v-model="draft.umamiApiKey" class="form-control" type="password"
                                   autocomplete="off" placeholder="umami_…" :disabled="isSaving" @input="isDirty = true">
                            <button v-if="config?.apiKeyConfigured" class="btn btn-outline-danger text-nowrap"
                                    type="button" :disabled="isSaving || isRemovingKey" @click="handleRemoveApiKey">
                              <span v-if="isRemovingKey" class="spinner-border spinner-border-sm me-1"></span>
                              Remove key
                            </button>
                          </div>
                          <div class="form-text text-muted">
                            <template v-if="config?.apiKeyConfigured">
                              A key is stored (ending <code>{{ config.apiKeyLast4 }}</code>). Leave blank to keep it;
                              type a new value to replace it, or use <em>Remove key</em> to delete it. The stored value
                              is encrypted at rest and is never displayed or returned to the browser.
                            </template>
                            <template v-else>
                              Write-only: used server-side to query your analytics, never sent to browsers.
                            </template>
                          </div>
                        </div>
                      </template>

                      <!-- GA4-specific fields -->
                      <template v-if="draft.provider === 'GA4'">
                        <div class="col-md-8">
                          <label class="form-label" for="ownGa4MeasurementId">GA4 Measurement ID</label>
                          <input id="ownGa4MeasurementId" v-model="draft.ga4MeasurementId" class="form-control" type="text"
                                 placeholder="G-XXXXXXXXXX" :disabled="isSaving" @input="isDirty = true">
                          <div class="form-text text-muted">
                            Public Measurement ID from your Google Analytics property. Tracking is injected on your
                            public pages; GA4 reporting dashboards are not implemented yet.
                          </div>
                        </div>
                      </template>

                      <div class="col-md-6 d-flex align-items-end">
                        <div class="w-100">
                          <div class="form-check form-switch">
                            <input id="ownTracking" v-model="draft.trackingEnabled" class="form-check-input"
                                   role="switch" type="checkbox" :disabled="isSaving" @change="isDirty = true">
                            <label class="form-check-label" for="ownTracking">Tracking on my public pages</label>
                          </div>
                          <div class="form-check form-switch">
                            <input id="ownReporting" v-model="draft.reportingEnabled" class="form-check-input"
                                   role="switch" type="checkbox" :disabled="isSaving || draft.provider === 'GA4'">
                            <label class="form-check-label" for="ownReporting">Reporting (my dashboard)</label>
                          </div>
                          <div v-if="draft.provider === 'GA4'" class="form-text text-muted">
                            Not available for GA4 yet.
                          </div>
                          <div class="form-check form-switch" v-if="draft.provider === 'UMAMI'">
                            <input id="ownRecorder" v-model="draft.recorderEnabled" class="form-check-input"
                                   role="switch" type="checkbox" :disabled="isSaving" @change="isDirty = true">
                            <label class="form-check-label" for="ownRecorder">Session recorder (experimental)</label>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="card-footer text-end border-0 bg-transparent">
                  <button :disabled="!isDirty || isSaving" class="btn btn-outline-light me-2" type="button"
                          @click="handleTestConnection">
                    <span v-if="isTesting" class="spinner-border spinner-border-sm me-2"></span>
                    Test Connection
                  </button>
                  <button :disabled="!isDirty || isSaving" class="btn btn-primary" type="submit">
                    {{ isSaving ? 'Saving...' : 'Save Configuration' }}
                  </button>
                </div>
              </div>
            </form>

            <!-- Connection test result -->
            <div v-if="testResult" class="mb-4">
              <div :class="testResult.success ? 'alert alert-success' : 'alert alert-warning'" class="mb-0">
                <strong>{{ testResult.success ? 'Connection OK:' : 'Connection failed:' }}</strong>
                {{ testResult.message }}
              </div>
            </div>

            <!-- Dashboard -->
            <div class="card glass-card shadow-sm">
              <div class="card-header d-flex justify-content-between align-items-center flex-wrap gap-2">
                <h5 class="mb-0">My Portfolio Analytics</h5>
                <select v-model="selectedPeriod" class="form-select form-select-sm w-auto"
                        :disabled="!dashboardAvailable || isLoadingDashboard" @change="loadDashboard">
                  <option value="24h">Last 24 hours</option>
                  <option value="7d">Last 7 days</option>
                  <option value="30d">Last 30 days</option>
                  <option value="90d">Last 90 days</option>
                  <option value="12month">Last 12 months</option>
                </select>
              </div>
              <div class="card-body p-4">
                <div v-if="!dashboardAvailable" class="text-center py-5">
                  <BarChart3 class="display-1 text-primary mb-3"/>
                  <h5 class="glass-title">No analytics dashboard</h5>
                  <p class="text-muted mb-0">
                    <template v-if="draft.mode === 'DISABLED'">
                      Analytics is disabled for your portfolio.
                    </template>
                    <template v-else-if="config && !config.effectiveReportingEnabled">
                      Reporting is not available for your current configuration.
                    </template>
                    <template v-else>
                      Enable reporting (and store an API key when using your own instance) to see
                      your portfolio's visitor analytics here.
                    </template>
                  </p>
                </div>

                <div v-else-if="isLoadingDashboard" class="text-center py-5">
                  <div class="spinner-border text-primary mb-3" role="status"></div>
                  <p class="text-muted">Loading analytics…</p>
                </div>

                <div v-else-if="dashboardError" class="alert alert-warning mb-0">
                  <h6 class="alert-heading">Analytics temporarily unavailable</h6>
                  <p class="mb-0">{{ dashboardError }}</p>
                  <button class="btn btn-sm btn-outline-dark mt-2" type="button" @click="loadDashboard">Retry</button>
                </div>

                <template v-else-if="summary">
                  <div class="row g-3 mb-4">
                    <div v-for="tile in summaryTiles" :key="tile.label" class="col-6 col-md-3">
                      <div class="stat-tile">
                        <div class="stat-value">{{ tile.value }}</div>
                        <div class="stat-label">{{ tile.label }}</div>
                      </div>
                    </div>
                  </div>

                  <h6 class="mb-3">Pageviews over time</h6>
                  <div v-if="!timeSeries || timeSeries.pageviews.length === 0" class="text-muted small mb-4">
                    No pageviews recorded in this period yet.
                  </div>
                  <div v-else class="chart-bars mb-4" role="img" aria-label="Pageviews over time">
                    <div v-for="point in normalizedSeries" :key="point.timestamp" class="chart-bar-wrapper"
                         :title="`${point.label}: ${point.value} pageviews`">
                      <div class="chart-bar" :style="{ height: point.pct + '%' }"></div>
                    </div>
                  </div>

                  <div class="row g-4">
                    <div v-for="metricBlock in metricBlocks" :key="metricBlock.title" class="col-md-6">
                      <h6 class="mb-3">{{ metricBlock.title }}</h6>
                      <div v-if="metricBlock.rows.length === 0" class="text-muted small">No data in this period.</div>
                      <ul v-else class="list-group list-group-flush metric-list">
                        <li v-for="row in metricBlock.rows" :key="row.name" class="list-group-item px-0 bg-transparent">
                          <div class="d-flex justify-content-between mb-1">
                            <span class="text-truncate me-2" :title="row.name">{{ row.displayName }}</span>
                            <span class="text-nowrap">{{ row.value }}</span>
                          </div>
                          <div class="metric-bar-track">
                            <div class="metric-bar" :style="{ width: row.pct + '%' }"></div>
                          </div>
                        </li>
                      </ul>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue';
import {Ban, BarChart3, Info} from 'lucide-vue-next';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import {
  AnalyticsConnectionTest,
  AnalyticsMetricRow,
  AnalyticsSummary,
  AnalyticsTimeSeries,
  getMyAnalyticsConfig,
  getMyAnalyticsMetrics,
  getMyAnalyticsSummary,
  getMyAnalyticsTimeseries,
  testMyAnalyticsConnection,
  updateMyAnalyticsConfig,
  UserAnalyticsConfig,
} from '@/services/api/analytics.api';

const isLoading = ref(true);
const isSaving = ref(false);
const isTesting = ref(false);
const loadError = ref(false);
const isDirty = ref(false);

const config = ref<UserAnalyticsConfig | null>(null);
const draft = reactive({
  mode: 'INHERIT' as UserAnalyticsConfig['mode'],
  provider: 'UMAMI' as UserAnalyticsConfig['provider'],
  umamiBaseUrl: '',
  umamiWebsiteName: '',
  umamiWebsiteId: '',
  umamiScriptPath: '/script.js',
  umamiRecorderPath: '/recorder.js',
  ga4MeasurementId: '',
  umamiApiKey: '',
  trackingEnabled: true,
  reportingEnabled: true,
  recorderEnabled: false,
});

const testResult = ref<AnalyticsConnectionTest | null>(null);
const isRemovingKey = ref(false);

/**
 * P10: explicit credential removal (mirrors the admin page). The save flow
 * omits the key field when the textbox is left blank (blank = keep), so
 * removal sends an explicit empty string — the backend's documented "clear"
 * signal. The key is never displayed anywhere; only the masked last-4 is.
 */
const handleRemoveApiKey = async () => {
  if (!config.value?.apiKeyConfigured) return;
  if (!window.confirm(
    'Remove your stored Umami API key? Your analytics dashboard will stop working until a new key is saved '
    + '(tracking on your public pages is unaffected).')) {
    return;
  }
  isRemovingKey.value = true;
  try {
    // Partial update: only the credential field — all other fields are kept.
    config.value = await updateMyAnalyticsConfig({umamiApiKey: ''});
    syncDraftFromConfig();
    testResult.value = {
      success: true,
      message: 'Stored API key removed. Your analytics dashboard is disabled until a new key is saved.',
      providerStatus: null,
    };
  } catch (err: unknown) {
    console.error('API key removal failed', err);
    alert(err instanceof Error ? err.message : 'Failed to remove the stored API key.');
  } finally {
    isRemovingKey.value = false;
  }
};

const selectedPeriod = ref('30d');
const isLoadingDashboard = ref(false);
const dashboardError = ref('');
const summary = ref<AnalyticsSummary | null>(null);
const timeSeries = ref<AnalyticsTimeSeries | null>(null);
const topPages = ref<AnalyticsMetricRow[]>([]);
const referrers = ref<AnalyticsMetricRow[]>([]);
const countries = ref<AnalyticsMetricRow[]>([]);
const devices = ref<AnalyticsMetricRow[]>([]);

const modeBadgeText = computed(() => {
  if (!config.value) return 'Unknown';
  switch (config.value.effectiveProvider) {
    case 'NONE':
      return 'Analytics off';
    default:
      return config.value.mode === 'OWN' ? `Own instance (${config.value.effectiveProvider})` : 'Inheriting platform';
  }
});
const modeBadgeClass = computed(() =>
  config.value && config.value.effectiveProvider !== 'NONE' ? 'badge bg-success' : 'badge bg-secondary');

const dashboardAvailable = computed(() =>
  !!config.value && config.value.effectiveReportingEnabled);

const summaryTiles = computed(() => {
  if (!summary.value) return [];
  const s = summary.value;
  const fmtDuration = (secs: number) => {
    const avg = s.visits > 0 ? Math.round(secs / s.visits) : 0;
    return avg >= 60 ? `${Math.floor(avg / 60)}m ${avg % 60}s` : `${avg}s`;
  };
  return [
    {label: 'Pageviews', value: s.pageviews.toLocaleString()},
    {label: 'Unique visitors', value: s.visitors.toLocaleString()},
    {label: 'Visits', value: s.visits.toLocaleString()},
    {label: 'Avg. time on site', value: fmtDuration(s.totalTimeSeconds)},
  ];
});

interface SeriesPoint {
  timestamp: number;
  value: number;
  pct: number;
  label: string;
}

const normalizedSeries = computed<SeriesPoint[]>(() => {
  if (!timeSeries.value || timeSeries.value.pageviews.length === 0) return [];
  const points = timeSeries.value.pageviews;
  const max = Math.max(...points.map(p => p.value), 1);
  return points.map(p => ({
    timestamp: p.timestamp,
    value: p.value,
    pct: Math.max((p.value / max) * 100, 2),
    label: new Date(p.timestamp).toLocaleString(),
  }));
});

interface MetricBlock {
  title: string;
  rows: Array<{ name: string; displayName: string; value: number; pct: number }>;
}

const withPct = (rows: AnalyticsMetricRow[]): MetricBlock['rows'] => {
  const max = Math.max(...rows.map(r => r.value), 1);
  return rows.map(r => ({
    name: r.name,
    displayName: r.name && r.name.length > 60 ? r.name.slice(0, 60) + '…' : (r.name || '(direct/none)'),
    value: r.value,
    pct: (r.value / max) * 100,
  }));
};

const metricBlocks = computed<MetricBlock[]>(() => [
  {title: 'Top pages', rows: withPct(topPages.value)},
  {title: 'Top referrers', rows: withPct(referrers.value)},
  {title: 'Countries', rows: withPct(countries.value)},
  {title: 'Devices', rows: withPct(devices.value)},
]);

onMounted(async () => {
  try {
    config.value = await getMyAnalyticsConfig();
    syncDraftFromConfig();
    if (dashboardAvailable.value) {
      await loadDashboard();
    }
  } catch (err) {
    console.error('Failed to load analytics config', err);
    loadError.value = true;
  } finally {
    isLoading.value = false;
  }
});

const syncDraftFromConfig = () => {
  const c = config.value!;
  draft.mode = c.mode;
  draft.provider = c.provider === 'NONE' ? 'UMAMI' : c.provider;
  draft.umamiBaseUrl = c.umamiBaseUrl || '';
  draft.umamiWebsiteName = c.umamiWebsiteName || '';
  draft.umamiWebsiteId = c.umamiWebsiteId || '';
  draft.umamiScriptPath = c.umamiScriptPath || '/script.js';
  draft.umamiRecorderPath = c.umamiRecorderPath || '/recorder.js';
  draft.ga4MeasurementId = c.ga4MeasurementId || '';
  draft.umamiApiKey = '';
  draft.trackingEnabled = c.trackingEnabled;
  draft.reportingEnabled = c.reportingEnabled;
  draft.recorderEnabled = c.recorderEnabled;
  isDirty.value = false;
};

const loadDashboard = async () => {
  if (!dashboardAvailable.value) return;
  isLoadingDashboard.value = true;
  dashboardError.value = '';
  const params = {period: selectedPeriod.value};
  try {
    const [summaryRes, seriesRes, pages, refs, ctry, dev] = await Promise.all([
      getMyAnalyticsSummary(params),
      getMyAnalyticsTimeseries(params),
      getMyAnalyticsMetrics('PATH', params),
      getMyAnalyticsMetrics('REFERRER', params),
      getMyAnalyticsMetrics('COUNTRY', params),
      getMyAnalyticsMetrics('DEVICE', params),
    ]);
    summary.value = summaryRes;
    timeSeries.value = seriesRes;
    topPages.value = pages;
    referrers.value = refs;
    countries.value = ctry;
    devices.value = dev;
  } catch (err: any) {
    console.error('Failed to load analytics dashboard', err);
    dashboardError.value = err?.message || 'The analytics provider could not be reached.';
    summary.value = null;
    timeSeries.value = null;
  } finally {
    isLoadingDashboard.value = false;
  }
};

const handleSaveConfig = async () => {
  isSaving.value = true;
  try {
    const update: Record<string, unknown> = {
      mode: draft.mode,
      provider: draft.mode === 'OWN' ? draft.provider : undefined,
      trackingEnabled: draft.trackingEnabled,
      reportingEnabled: draft.mode === 'OWN' && draft.provider === 'GA4' ? false : draft.reportingEnabled,
      recorderEnabled: draft.mode === 'OWN' && draft.provider === 'UMAMI' ? draft.recorderEnabled : false,
      umamiBaseUrl: draft.umamiBaseUrl,
      umamiWebsiteName: draft.umamiWebsiteName,
      umamiWebsiteId: draft.umamiWebsiteId,
      umamiScriptPath: draft.umamiScriptPath,
      umamiRecorderPath: draft.umamiRecorderPath,
      ga4MeasurementId: draft.ga4MeasurementId,
    };
    if (draft.umamiApiKey !== '') {
      update.umamiApiKey = draft.umamiApiKey;
    }
    config.value = await updateMyAnalyticsConfig(update);
    syncDraftFromConfig();
    if (dashboardAvailable.value) {
      await loadDashboard();
    }
  } catch (err: any) {
    console.error('Save failed', err);
    alert(err?.message || 'Failed to save your analytics configuration.');
  } finally {
    isSaving.value = false;
  }
};

const handleTestConnection = async () => {
  isTesting.value = true;
  testResult.value = null;
  try {
    if (isDirty.value) {
      await handleSaveConfig();
    }
    testResult.value = await testMyAnalyticsConnection();
  } catch (err: any) {
    testResult.value = {success: false, message: err?.message || 'Connection test failed.', providerStatus: null};
  } finally {
    isTesting.value = false;
  }
};
</script>

<style scoped>
.stat-tile {
  background: rgba(var(--bs-body-color-rgb), 0.05);
  border: 1px solid var(--glass-border);
  border-radius: 0.5rem;
  padding: 0.9rem 1rem;
  height: 100%;
}
.stat-value {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--glass-text);
  line-height: 1.2;
}
.stat-label {
  font-size: 0.8rem;
  color: var(--glass-text-secondary);
}
.glass-title {
  color: var(--glass-text);
  font-weight: 700;
}
.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 140px;
  padding: 0.5rem;
  border: 1px solid var(--glass-border);
  border-radius: 0.5rem;
  background: rgba(var(--bs-body-color-rgb), 0.03);
}
.chart-bar-wrapper {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: flex-end;
}
.chart-bar {
  width: 100%;
  background: linear-gradient(180deg, var(--bs-primary) 0%, rgba(var(--bs-primary-rgb), 0.5) 100%);
  border-radius: 2px 2px 0 0;
  min-height: 2px;
}
.metric-list .list-group-item {
  border-color: var(--glass-border);
}
.metric-bar-track {
  height: 5px;
  background: rgba(var(--bs-body-color-rgb), 0.08);
  border-radius: 3px;
  overflow: hidden;
}
.metric-bar {
  height: 100%;
  background: var(--bs-primary);
  border-radius: 3px;
}
</style>
