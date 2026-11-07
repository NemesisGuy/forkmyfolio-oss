<script setup lang="ts">
import { computed } from 'vue';
import type { JobApplication } from '@/types';
import { STATUS_META } from '@/types';
import { MapPin, Calendar, Eye, Pencil, Trash2, ExternalLink, AlarmClock, CalendarClock } from 'lucide-vue-next';

const props = defineProps<{
  application: JobApplication;
}>();

defineEmits<{
  (e: 'view', uuid: string): void;
  (e: 'edit', uuid: string): void;
  (e: 'delete', uuid: string): void;
}>();

const statusMeta = computed(() => STATUS_META[props.application.status] ?? { label: props.application.status, badge: 'bg-secondary' });

const workArrangementLabel = computed(() => {
  switch ((props.application.workArrangement ?? '').toUpperCase()) {
    case 'REMOTE': return 'Remote';
    case 'HYBRID': return 'Hybrid';
    case 'ON_SITE': return 'On-site';
    default: return '';
  }
});

/** Days until a future date (negative = past). Null when absent. */
const daysUntil = (iso?: string) => {
  if (!iso) return null;
  const target = new Date(iso);
  if (Number.isNaN(target.getTime())) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  target.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86400000);
};

const deadlineInfo = computed(() => {
  const days = daysUntil(props.application.deadlineAt);
  if (days === null) return null;
  if (days < 0) return { text: `Deadline passed ${-days}d ago`, urgent: true, overdue: true };
  if (days === 0) return { text: 'Deadline TODAY', urgent: true, overdue: false };
  if (days <= 7) return { text: `Deadline in ${days}d`, urgent: true, overdue: false };
  return { text: `Deadline in ${days}d`, urgent: false, overdue: false };
});

const followUpInfo = computed(() => {
  const days = daysUntil(props.application.followUpAt);
  if (days === null) return null;
  if (days < 0) return { text: `Follow-up due (${(-days)}d ago)`, urgent: true };
  if (days <= 3) return { text: `Follow-up in ${days}d`, urgent: true };
  return { text: `Follow-up in ${days}d`, urgent: false };
});

const formattedApplied = computed(() => {
  const source = props.application.appliedAt ?? props.application.createdAt;
  return source ? new Date(source).toLocaleDateString() : '—';
});
</script>

<template>
  <div class="card h-100 shadow-sm glass-card" :class="{ 'archived-card': application.status === 'ARCHIVED' }">
    <div class="card-body">
      <div class="d-flex justify-content-between align-items-start mb-2">
        <h5 class="card-title text-truncate" :title="application.jobTitle">{{ application.jobTitle }}</h5>
        <span class="badge" :class="statusMeta.badge" data-testid="card-status">{{ statusMeta.label }}</span>
      </div>
      <h6 class="card-subtitle mb-2 text-muted">{{ application.companyName }}</h6>
      <p class="card-text small text-muted mb-1">
        <MapPin class="me-1" />{{ application.jobLocation || 'Location unknown' }}
        <span v-if="workArrangementLabel" class="ms-2 badge bg-light text-dark">{{ workArrangementLabel }}</span>
        <span v-if="application.jobType" class="ms-2 badge bg-light text-dark">{{ application.jobType }}</span>
      </p>
      <p class="card-text small text-muted mb-1">
        <Calendar class="me-1" />Applied: {{ formattedApplied }}
      </p>
      <p v-if="application.salaryRange" class="card-text small text-muted mb-1">
        💰 {{ application.salaryRange }}<span v-if="application.salaryCurrency"> {{ application.salaryCurrency }}</span><span v-if="application.salaryPeriod"> / {{ application.salaryPeriod.toLowerCase() }}</span>
      </p>

      <!-- Deadline / follow-up indicators -->
      <div v-if="deadlineInfo || followUpInfo" class="mt-2">
        <span v-if="deadlineInfo" class="badge me-1"
              :class="deadlineInfo.urgent ? (deadlineInfo.overdue ? 'bg-danger' : 'bg-warning text-dark') : 'bg-light text-dark'"
              data-testid="deadline-indicator">
          <CalendarClock class="me-1" style="width: 12px; height: 12px;" />{{ deadlineInfo.text }}
        </span>
        <span v-if="followUpInfo" class="badge"
              :class="followUpInfo.urgent ? 'bg-warning text-dark' : 'bg-light text-dark'"
              data-testid="followup-indicator">
          <AlarmClock class="me-1" style="width: 12px; height: 12px;" />{{ followUpInfo.text }}
        </span>
      </div>
    </div>
    <div class="card-footer bg-transparent border-top-0 d-flex justify-content-end gap-2">
      <a
        v-if="application.applicationUrl"
        :href="application.applicationUrl"
        class="btn btn-sm btn-outline-primary"
        target="_blank"
        rel="noopener noreferrer"
        title="Open job posting">
        <ExternalLink />
      </a>
      <button class="btn btn-sm btn-outline-info" @click="$emit('view', application.uuid)" title="View Details">
        <Eye />
      </button>
      <button class="btn btn-sm btn-outline-secondary" @click="$emit('edit', application.uuid)" title="Edit">
        <Pencil />
      </button>
      <button class="btn btn-sm btn-outline-danger" @click="$emit('delete', application.uuid)" title="Delete">
        <Trash2 />
      </button>
    </div>
  </div>
</template>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px rgba(var(--bs-body-color-rgb), 0.3);
  border-style: solid;
  transition: transform 0.2s, box-shadow 0.2s;
}

.glass-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.archived-card {
  opacity: 0.75;
  border-style: dashed;
}
</style>
