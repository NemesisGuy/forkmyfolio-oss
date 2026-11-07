<template>
  <div class="admin-stats-page py-5 flex-grow-1 d-flex flex-column">
    <div class="container">
      <div class="d-flex justify-content-between align-items-center mb-4 animate-fade-in-up">
        <h2 class="glass-text mb-0">System Analytics</h2>
      </div>

      <LoadingModal :visible="isLoading || isSaving || isTesting"/>
      <SuccessModal
        :visible="showSuccessModal"
        message="Analytics settings updated successfully."
        title="Settings Saved"
        @close="showSuccessModal = false"
      />
      <ErrorModal
        :message="errorMessage"
        :visible="showErrorModal"
        title="Save Failed"
        @close="showErrorModal = false"
      />

      <!-- Analytics Configuration Card -->
      <div class="row g-4 mb-5 animate-fade-in-up">
        <div class="col-12">
          <form @submit.prevent="handleSaveSettings">
            <div class="card glass-card">
              <div class="card-header d-flex justify-content-between align-items-center">
                <h5 class="mb-0">Analytics Configuration</h5>
                <span :class="configBadgeClass">{{ configBadgeText }}</span>
              </div>
              <div class="card-body p-4">
                <p class="glass-subtitle mb-4">
                  Configure the platform's analytics provider. Tracking injects a privacy-friendly
                  script into public portfolio pages; reporting queries the provider's API
                  server-side. Credentials are stored encrypted and are never exposed to browsers.
                </p>

                <div v-if="loadError" class="alert alert-danger">
                  Could not load the analytics configuration. Please try again later.
                </div>

                <template v-else>
                  <div class="row g-3">
                    <div class="col-md-4">
                      <label class="form-label glass-text" for="analyticsProvider">Provider</label>
                      <select id="analyticsProvider" v-model="draft.provider" class="form-select glass-input"
                              :disabled="isSaving" @change="isDirty = true">
                        <option value="NONE">None (disabled)</option>
                        <option value="UMAMI">Umami</option>
                        <option value="GA4">Google Analytics 4</option>
                      </select>
                    </div>

                    <!-- Umami-specific configuration -->
                    <template v-if="draft.provider === 'UMAMI'">
                      <div class="col-md-8">
                        <label class="form-label glass-text" for="umamiBaseUrl">Umami Base URL</label>
                        <input
                          id="umamiBaseUrl"
                          v-model="draft.umamiBaseUrl"
                          class="form-control glass-input"
                          placeholder="https://metrics.example.com"
                          type="url"
                          :disabled="isSaving"
                          @input="isDirty = true"
                        />
                        <div class="form-text text-muted">Root URL of your Umami instance (https required).</div>
                      </div>
                      <div class="col-md-4">
                        <label class="form-label glass-text" for="umamiWebsiteName">Website Name <span class="text-muted small">(optional)</span></label>
                        <input
                          id="umamiWebsiteName"
                          v-model="draft.umamiWebsiteName"
                          class="form-control glass-input"
                          placeholder="e.g. ForkMyFolio"
                          type="text"
                          :disabled="isSaving"
                          @input="isDirty = true"
                        />
                        <div class="form-text text-muted">Display label only — helps you identify the site in Umami.</div>
                      </div>
                      <div class="col-md-4">
                        <label class="form-label glass-text" for="umamiWebsiteId">Master Website ID</label>
                        <input
                          id="umamiWebsiteId"
                          v-model="draft.umamiWebsiteId"
                          class="form-control glass-input"
                          placeholder="e.g. 54483748-3843-..."
                          type="text"
                          :disabled="isSaving"
                          @input="isDirty = true"
                        />
                        <div class="form-text text-muted">Public UUID of the site-wide website in Umami.</div>
                      </div>
                      <div class="col-md-4">
                        <label class="form-label glass-text" for="umamiScriptPath">Tracking Script Path</label>
                        <input
                          id="umamiScriptPath"
                          v-model="draft.umamiScriptPath"
                          class="form-control glass-input"
                          placeholder="/script.js"
                          type="text"
                          :disabled="isSaving"
                          @input="isDirty = true"
                        />
                        <div class="form-text text-muted">Path on your Umami instance (default <code>/script.js</code>).</div>
                      </div>
                      <div class="col-md-4">
                        <label class="form-label glass-text" for="umamiRecorderPath">Recorder Script Path</label>
                        <input
                          id="umamiRecorderPath"
                          v-model="draft.umamiRecorderPath"
                          class="form-control glass-input"
                          placeholder="/recorder.js"
                          type="text"
                          :disabled="isSaving"
                          @input="isDirty = true"
                        />
                        <div class="form-text text-muted">Path on your Umami instance (default <code>/recorder.js</code>).</div>
                      </div>
                      <div class="col-md-6">
                        <label class="form-label glass-text" for="umamiApiKey">
                          Umami API Key
                          <span class="badge ms-1 align-middle"
                                :class="config?.apiKeyConfigured ? 'bg-success' : 'bg-secondary'">
                            {{ config?.apiKeyConfigured ? `stored ••••${config.apiKeyLast4}` : 'not stored' }}
                          </span>
                        </label>
                        <div class="d-flex gap-2">
                          <input
                            id="umamiApiKey"
                            v-model="draft.umamiApiKey"
                            autocomplete="off"
                            class="form-control glass-input"
                            placeholder="umami_…"
                            type="password"
                            :disabled="isSaving"
                            @input="isDirty = true"
                          />
                          <button
                            v-if="config?.apiKeyConfigured"
                            class="btn btn-outline-danger text-nowrap"
                            type="button"
                            :disabled="isSaving || isRemovingKey"
                            @click="handleRemoveApiKey"
                          >
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
                            Write-only: used server-side for reporting, never sent to browsers.
                          </template>
                          <template v-if="config && !config.credentialStorageAvailable">
                            <br><span class="text-warning">Credential storage is unavailable on this server
                            (missing ANALYTICS_ENCRYPTION_KEY) — reporting cannot be enabled.</span>
                          </template>
                        </div>
                      </div>
                    </template>

                    <!-- GA4-specific configuration -->
                    <template v-if="draft.provider === 'GA4'">
                      <div class="col-md-6">
                        <label class="form-label glass-text" for="ga4MeasurementId">GA4 Measurement ID</label>
                        <input
                          id="ga4MeasurementId"
                          v-model="draft.ga4MeasurementId"
                          class="form-control glass-input"
                          placeholder="G-XXXXXXXXXX"
                          type="text"
                          :disabled="isSaving"
                          @input="isDirty = true"
                        />
                        <div class="form-text text-muted">
                          Public Measurement ID from your Google Analytics property (format <code>G-XXXXXXXXXX</code>).
                          Tracking is injected on public pages; GA4 reporting dashboards are not implemented yet.
                        </div>
                      </div>
                    </template>
                  </div>

                  <div class="row g-3 mt-1">
                    <div class="col-md-4">
                      <div class="form-check form-switch">
                        <input id="trackingEnabled" v-model="draft.trackingEnabled" class="form-check-input"
                               role="switch" type="checkbox" :disabled="isSaving" @change="isDirty = true">
                        <label class="form-check-label glass-text" for="trackingEnabled">Tracking (public pages)</label>
                      </div>
                    </div>
                    <div class="col-md-4">
                      <div class="form-check form-switch">
                        <input id="reportingEnabled" v-model="draft.reportingEnabled" class="form-check-input"
                               role="switch" type="checkbox" :disabled="isSaving || draft.provider === 'GA4'">
                        <label class="form-check-label glass-text" for="reportingEnabled">Reporting (dashboards)</label>
                      </div>
                      <div v-if="draft.provider === 'GA4'" class="form-text text-muted">
                        Not available for GA4 yet.
                      </div>
                    </div>
                    <div class="col-md-4">
                      <div class="form-check form-switch">
                        <input id="recorderEnabled" v-model="draft.recorderEnabled" class="form-check-input"
                               role="switch" type="checkbox" :disabled="isSaving || draft.provider !== 'UMAMI'">
                        <label class="form-check-label glass-text" for="recorderEnabled">Session recorder (experimental)</label>
                      </div>
                      <div class="form-text text-muted">
                        Loads the recorder script on public pages only. Inputs are masked; never
                        loaded on CMS/admin pages. Review Umami's retention settings before enabling.
                      </div>
                    </div>
                  </div>
                </template>
              </div>
              <div class="card-footer text-end border-0 bg-transparent">
                <button :disabled="!isDirty || isSaving" class="btn btn-outline-light me-2"
                        type="button" @click="handleTestConnection">
                  <span v-if="isTesting" class="spinner-border spinner-border-sm me-2"></span>
                  Test Connection
                </button>
                <button :disabled="!isDirty || isSaving" class="btn btn-primary" type="submit">
                  <span v-if="isSaving" class="spinner-border spinner-border-sm me-2"></span>
                  {{ isSaving ? 'Saving...' : 'Save Configuration' }}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>

      <!-- Connection test result -->
      <div v-if="testResult" class="row g-4 mb-4 animate-fade-in-up">
        <div class="col-12">
          <div :class="testResult.success ? 'alert alert-success' : 'alert alert-warning'" class="mb-0">
            <strong>{{ testResult.success ? 'Connection OK:' : 'Connection failed:' }}</strong>
            {{ testResult.message }}
          </div>
        </div>
      </div>

      <!-- Master Analytics Dashboard -->
      <div class="row g-4 animate-fade-in-up" style="animation-delay: 0.1s;">
        <div class="col-12">
          <div class="card glass-card">
            <div class="card-header d-flex justify-content-between align-items-center flex-wrap gap-2">
              <h5 class="mb-0">Site-wide Analytics</h5>
              <select v-model="selectedPeriod" class="form-select form-select-sm w-auto glass-input"
                      :disabled="!dashboardAvailable || isLoadingDashboard" @change="loadDashboard">
                <option value="24h">Last 24 hours</option>
                <option value="7d">Last 7 days</option>
                <option value="30d">Last 30 days</option>
                <option value="90d">Last 90 days</option>
                <option value="12month">Last 12 months</option>
              </select>
            </div>
            <div class="card-body p-4">
              <!-- Dashboard unavailable -->
              <div v-if="!dashboardAvailable" class="text-center py-5">
                <BarChart3 class="display-1 text-primary mb-3"/>
                <h5 class="glass-title">No analytics dashboard yet</h5>
                <p class="glass-subtitle mb-4">
                  Enable the Umami provider with reporting above to see site-wide visitor analytics here.
                </p>
                <div class="d-flex justify-content-center gap-3">
                  <a href="https://umami.is/docs" target="_blank" class="btn btn-outline-light interactive-lift">
                    <Info class="me-2"/>Umami Documentation
                  </a>
                </div>
              </div>

              <!-- Loading -->
              <div v-else-if="isLoadingDashboard" class="text-center py-5">
                <div class="spinner-border text-primary mb-3" role="status"></div>
                <p class="glass-subtitle">Loading analytics…</p>
              </div>

              <!-- Provider unreachable / error -->
              <div v-else-if="dashboardError" class="alert alert-warning mb-0">
                <h6 class="alert-heading">Analytics temporarily unavailable</h6>
                <p class="mb-0">{{ dashboardError }}</p>
                <button class="btn btn-sm btn-outline-dark mt-2" type="button" @click="loadDashboard">Retry</button>
              </div>

              <!-- Dashboard -->
              <template v-else-if="summary">
                <!-- Summary tiles -->
                <div class="row g-3 mb-4">
                  <div v-for="tile in summaryTiles" :key="tile.label" class="col-6 col-md-3 col-lg-2">
                    <div class="stat-tile">
                      <div class="stat-value">{{ tile.value }}</div>
                      <div class="stat-label">{{ tile.label }}</div>
                      <div v-if="tile.delta !== null" class="stat-delta" :class="tile.delta >= 0 ? 'text-success' : 'text-danger'">
                        {{ tile.delta >= 0 ? '▲' : '▼' }} {{ Math.abs(tile.delta) }}%
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Time series -->
                <h6 class="glass-text mb-3">Pageviews over time</h6>
                <div v-if="!timeSeries || timeSeries.pageviews.length === 0" class="text-muted small mb-4">
                  No pageviews recorded in this period yet.
                </div>
                <div v-else class="chart-bars mb-4" role="img" aria-label="Pageviews over time">
                  <div v-for="point in normalizedSeries" :key="point.timestamp" class="chart-bar-wrapper"
                       :title="`${point.label}: ${point.value} pageviews`">
                    <div class="chart-bar" :style="{ height: point.pct + '%' }"></div>
                  </div>
                </div>
                <div v-if="timeSeries && timeSeries.pageviews.length > 0" class="d-flex justify-content-between text-muted small mb-4">
                  <span>{{ seriesStartLabel }}</span>
                  <span>{{ seriesEndLabel }}</span>
                </div>

                <!-- Metrics rows -->
                <div class="row g-4">
                  <div v-for="metricBlock in metricBlocks" :key="metricBlock.title" class="col-md-6">
                    <h6 class="glass-text mb-3">{{ metricBlock.title }}</h6>
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue';
import {BarChart3, Info} from 'lucide-vue-next';
import {
  AnalyticsConfig,
  AnalyticsConnectionTest,
  AnalyticsMetricRow,
  AnalyticsMetricType,
  AnalyticsSummary,
  AnalyticsTimeSeries,
  getAdminAnalyticsMetrics,
  getAdminAnalyticsSummary,
  getAdminAnalyticsTimeseries,
  getAnalyticsConfig,
  testAnalyticsConnection,
  updateAnalyticsConfig,
} from '@/services/api/analytics.api';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import SuccessModal from '@/components/common/modals/SuccessModal.vue';
import ErrorModal from '@/components/common/modals/ErrorModal.vue';

const isLoading = ref(true);
const isSaving = ref(false);
const isTesting = ref(false);
const showSuccessModal = ref(false);
const showErrorModal = ref(false);
const errorMessage = ref('');
const loadError = ref(false);
const isDirty = ref(false);

const config = ref<AnalyticsConfig | null>(null);
const draft = reactive({
  provider: 'NONE' as AnalyticsConfig['provider'],
  umamiBaseUrl: '',
  umamiWebsiteName: '',
  umamiWebsiteId: '',
  umamiScriptPath: '/script.js',
  umamiRecorderPath: '/recorder.js',
  ga4MeasurementId: '',
  umamiApiKey: '',
  trackingEnabled: false,
  reportingEnabled: false,
  recorderEnabled: false,
});

const testResult = ref<AnalyticsConnectionTest | null>(null);
const isRemovingKey = ref(false);

/**
 * P10: explicit credential removal. The save flow deliberately omits the key
 * field when the textbox is left blank (blank = keep), so removal needs its
 * own action that sends an empty string — the backend's documented "clear"
 * signal. The key is never displayed anywhere; only the masked last-4 is.
 */
const handleRemoveApiKey = async () => {
  if (!config.value?.apiKeyConfigured) return;
  if (!window.confirm(
    'Remove the stored Umami API key? Reporting dashboards will stop working until a new key is saved '
    + '(tracking on public pages is unaffected).')) {
    return;
  }
  isRemovingKey.value = true;
  try {
    // Partial update: only the credential field — all other fields are kept.
    config.value = await updateAnalyticsConfig({umamiApiKey: ''});
    syncDraftFromConfig();
    testResult.value = {
      success: true,
      message: 'Stored API key removed. Reporting dashboards are disabled until a new key is saved.',
      providerStatus: null,
    };
  } catch (err: unknown) {
    console.error('API key removal failed', err);
    alert(err instanceof Error ? err.message : 'Failed to remove the stored API key.');
  } finally {
    isRemovingKey.value = false;
  }
};

// Dashboard state
const selectedPeriod = ref('30d');
const isLoadingDashboard = ref(false);
const dashboardError = ref('');
const summary = ref<AnalyticsSummary | null>(null);
const timeSeries = ref<AnalyticsTimeSeries | null>(null);
const topPages = ref<AnalyticsMetricRow[]>([]);
const referrers = ref<AnalyticsMetricRow[]>([]);
const countries = ref<AnalyticsMetricRow[]>([]);
const browsers = ref<AnalyticsMetricRow[]>([]);
const devices = ref<AnalyticsMetricRow[]>([]);

const configBadgeText = computed(() => {
  if (!config.value) return 'Unknown';
  if (config.value.provider === 'NONE') return 'Disabled';
  if (!config.value.apiKeyConfigured) return 'Tracking only (no API key)';
  if (config.value.trackingEnabled && config.value.reportingEnabled) return 'Tracking + Reporting';
  if (config.value.trackingEnabled) return 'Tracking only';
  if (config.value.reportingEnabled) return 'Reporting only';
  return 'Configured (inactive)';
});
const configBadgeClass = computed(() =>
  config.value && config.value.provider !== 'NONE' ? 'badge bg-success' : 'badge bg-secondary');

const dashboardAvailable = computed(() =>
  !!config.value
  && config.value.provider !== 'NONE'
  && config.value.reportingEnabled
  && config.value.apiKeyConfigured);

const summaryTiles = computed(() => {
  if (!summary.value) return [];
  const s = summary.value;
  const delta = (now: number, prev?: number) =>
    prev === undefined || prev === 0 ? null : Math.round(((now - prev) / prev) * 100);
  const fmtDuration = (secs: number) => {
    const avg = s.visits > 0 ? Math.round(secs / s.visits) : 0;
    return avg >= 60 ? `${Math.floor(avg / 60)}m ${avg % 60}s` : `${avg}s`;
  };
  return [
    {label: 'Pageviews', value: s.pageviews.toLocaleString(), delta: delta(s.pageviews, s.comparison?.pageviews)},
    {label: 'Unique visitors', value: s.visitors.toLocaleString(), delta: delta(s.visitors, s.comparison?.visitors)},
    {label: 'Visits', value: s.visits.toLocaleString(), delta: delta(s.visits, s.comparison?.visits)},
    {label: 'Bounce rate', value: s.visits > 0 ? `${Math.round((s.bounces / s.visits) * 100)}%` : '0%', delta: null},
    {label: 'Avg. time on site', value: fmtDuration(s.totalTimeSeconds), delta: null},
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

const seriesStartLabel = computed(() =>
  normalizedSeries.value.length ? new Date(normalizedSeries.value[0].timestamp).toLocaleDateString() : '');
const seriesEndLabel = computed(() =>
  normalizedSeries.value.length
    ? new Date(normalizedSeries.value[normalizedSeries.value.length - 1].timestamp).toLocaleDateString()
    : '');

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
  {title: 'Browsers', rows: withPct(browsers.value)},
  {title: 'Devices', rows: withPct(devices.value)},
]);

onMounted(async () => {
  try {
    config.value = await getAnalyticsConfig();
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
  draft.provider = c.provider;
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
    const [summaryRes, seriesRes, pages, refs, ctry, brws, dev] = await Promise.all([
      getAdminAnalyticsSummary(params),
      getAdminAnalyticsTimeseries(params),
      getAdminAnalyticsMetrics('PATH', params),
      getAdminAnalyticsMetrics('REFERRER', params),
      getAdminAnalyticsMetrics('COUNTRY', params),
      getAdminAnalyticsMetrics('BROWSER', params),
      getAdminAnalyticsMetrics('DEVICE', params),
    ]);
    summary.value = summaryRes;
    timeSeries.value = seriesRes;
    topPages.value = pages;
    referrers.value = refs;
    countries.value = ctry;
    browsers.value = brws;
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

const handleSaveSettings = async () => {
  isSaving.value = true;
  try {
    const update: Record<string, unknown> = {
      provider: draft.provider,
      trackingEnabled: draft.trackingEnabled,
      reportingEnabled: draft.provider === 'GA4' ? false : draft.reportingEnabled,
      recorderEnabled: draft.provider === 'UMAMI' ? draft.recorderEnabled : false,
      umamiBaseUrl: draft.umamiBaseUrl,
      umamiWebsiteName: draft.umamiWebsiteName,
      umamiWebsiteId: draft.umamiWebsiteId,
      umamiScriptPath: draft.umamiScriptPath,
      umamiRecorderPath: draft.umamiRecorderPath,
      ga4MeasurementId: draft.ga4MeasurementId,
    };
    // Write-only credential semantics: empty string clears, value replaces, undefined keeps.
    if (draft.umamiApiKey !== '') {
      update.umamiApiKey = draft.umamiApiKey;
    }
    config.value = await updateAnalyticsConfig(update);
    syncDraftFromConfig();
    showSuccessModal.value = true;
    if (dashboardAvailable.value) {
      await loadDashboard();
    }
  } catch (err: any) {
    console.error('Save failed', err);
    errorMessage.value = err?.message || 'Failed to save configuration.';
    showErrorModal.value = true;
  } finally {
    isSaving.value = false;
  }
};

const handleTestConnection = async () => {
  isTesting.value = true;
  testResult.value = null;
  try {
    // If the admin changed connection fields without saving, save first so the
    // test runs against what will actually be stored.
    if (isDirty.value) {
      await handleSaveSettings();
    }
    testResult.value = await testAnalyticsConnection();
  } catch (err: any) {
    testResult.value = {success: false, message: err?.message || 'Connection test failed.', providerStatus: null};
  } finally {
    isTesting.value = false;
  }
};
</script>

<style scoped>
.glass-text {
  color: var(--glass-text);
}
.glass-title {
  color: var(--glass-text);
  font-weight: 700;
}
.glass-subtitle {
  color: var(--glass-text-secondary);
}
.glass-input {
  background: rgba(var(--bs-body-color-rgb), 0.05);
  border: 1px solid var(--glass-border);
  color: var(--glass-text);
}
.glass-input:focus {
  background: rgba(var(--bs-body-color-rgb), 0.1);
  border-color: var(--glass-border-hover);
  color: var(--glass-text);
  box-shadow: none;
}
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
.stat-delta {
  font-size: 0.75rem;
  margin-top: 0.15rem;
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
