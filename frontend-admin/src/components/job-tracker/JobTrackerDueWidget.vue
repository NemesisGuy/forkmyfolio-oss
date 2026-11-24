<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { jobApplicationService } from '@/services/jobApplicationService';
import { authService } from '@/services/authService';
import type { JobApplication, ApplicationStatus } from '@/types';
import { CalendarClock, AlarmClock, Briefcase } from 'lucide-vue-next';

/**
 * Phase 0.32 (TASKS §I2): "Needs attention" dashboard widget.
 *
 * Surfaces job applications whose dates are due TODAY or already overdue —
 * deadlines, follow-ups, interviews and assessments — which until now were
 * stored but never shown anywhere except inside each card on the tracker
 * page itself.
 *
 * Noise rules (pinned by tests):
 *  - terminal statuses (rejected / withdrawn / accepted / archived) never
 *    raise a deadline or follow-up alert — the application is finished;
 *  - a past interview/assessment date only alerts while the application is
 *    still waiting for it (INTERVIEW_SCHEDULED / INTERVIEWING /
 *    TECHNICAL_ASSESSMENT) — i.e. the user forgot to update the status;
 *  - future dates never alert (upcoming items live on the tracker cards);
 *  - the widget hides itself entirely when there is nothing to act on, and
 *    stays silent if the API fails (the dashboard must never break).
 */

const applications = ref<JobApplication[]>([]);
const loaded = ref(false);

const TERMINAL_STATUSES: ApplicationStatus[] = ['REJECTED', 'WITHDRAWN', 'ACCEPTED', 'ARCHIVED'];

/** Interview/assessment dates only alert while still awaited. */
const AWAITING_INTERVIEW: ApplicationStatus[] = [
  'INTERESTED', 'PREPARING', 'DRAFT', 'APPLIED', 'ACKNOWLEDGED',
  'INTERVIEW_SCHEDULED', 'INTERVIEWING', 'TECHNICAL_ASSESSMENT',
];

interface DueItem {
  uuid: string;
  jobTitle: string;
  companyName: string;
  kind: 'Deadline' | 'Follow-up' | 'Interview' | 'Assessment';
  date: string;
  days: number; // 0 = today, negative = overdue
}

/** Whole-day difference from today (0 = today, negative = past). Null when absent/invalid. */
const daysUntil = (iso?: string): number | null => {
  if (!iso) return null;
  const target = new Date(iso);
  if (Number.isNaN(target.getTime())) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  target.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86400000);
};

const dueItems = computed<DueItem[]>(() => {
  const items: DueItem[] = [];

  for (const app of applications.value) {
    if (TERMINAL_STATUSES.includes(app.status)) continue;

    const candidates: Array<{ kind: DueItem['kind']; iso?: string; awaitingOnly?: boolean }> = [
      { kind: 'Deadline', iso: app.deadlineAt },
      { kind: 'Follow-up', iso: app.followUpAt },
      { kind: 'Interview', iso: app.interviewAt, awaitingOnly: true },
      { kind: 'Assessment', iso: app.assessmentAt, awaitingOnly: true },
    ];

    for (const c of candidates) {
      const days = daysUntil(c.iso);
      if (days === null || days > 0) continue; // only due today or overdue
      if (c.awaitingOnly && !AWAITING_INTERVIEW.includes(app.status)) continue;
      items.push({
        uuid: app.uuid,
        jobTitle: app.jobTitle,
        companyName: app.companyName,
        kind: c.kind,
        date: (c.iso as string).slice(0, 10),
        days,
      });
    }
  }

  // Soonest first; overdue (more negative) first within the same day.
  return items.sort((a, b) => a.date.localeCompare(b.date));
});

const slug = computed(() => (authService.user.value as { slug?: string } | null)?.slug ?? '');

const urgencyClass = (days: number) => (days < 0 ? 'bg-danger' : 'bg-warning text-dark');

const dueLabel = (days: number) => (days === 0 ? 'today' : `${-days}d overdue`);

onMounted(async () => {
  try {
    applications.value = await jobApplicationService.getAll();
  } catch (error) {
    console.error('Due-widget: could not load job applications', error);
  } finally {
    loaded.value = true;
  }
});
</script>

<template>
  <div v-if="loaded && dueItems.length > 0" class="card glass-card" data-testid="due-widget">
    <div class="card-body">
      <h5 class="card-title mb-2">
        <CalendarClock class="me-2" />Needs attention
        <span class="badge bg-danger ms-1" data-testid="due-count">{{ dueItems.length }}</span>
      </h5>
      <ul class="list-unstyled mb-2" data-testid="due-list">
        <li
          v-for="(item, idx) in dueItems"
          :key="`${item.uuid}-${item.kind}`"
          class="d-flex align-items-center justify-content-between gap-2 py-1 border-bottom"
          :data-testid="`due-item-${idx}`"
        >
          <span class="small text-truncate">
            <Briefcase class="me-1" :size="14" style="vertical-align: -2px;" />
            <strong>{{ item.jobTitle }}</strong>
            <span class="text-muted"> — {{ item.companyName }}</span>
          </span>
          <span class="d-flex align-items-center gap-2 flex-shrink-0">
            <span class="badge" :class="urgencyClass(item.days)" data-testid="due-badge">
              <AlarmClock class="me-1" :size="12" style="vertical-align: -1px;" />
              {{ item.kind }} {{ dueLabel(item.days) }}
            </span>
            <span class="small text-muted">{{ item.date }}</span>
          </span>
        </li>
      </ul>
      <router-link
        :to="{ name: 'job-tracker', params: { slug } }"
        class="btn btn-sm btn-outline-primary"
        data-testid="due-open-tracker"
      >
        Open Job Tracker
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px rgba(var(--bs-body-color-rgb), 0.3);
  border-style: solid;
}
</style>
