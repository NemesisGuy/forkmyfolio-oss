<script setup lang="ts">
import { ref, computed } from 'vue';
import type { JobApplication, ApplicationStatus } from '@/types';
import { STATUS_META } from '@/types';
import { Eye, Pencil, Info } from 'lucide-vue-next';

/**
 * Phase 0.32 (TASKS §I2): kanban board view for the job tracker.
 *
 * The 15 lifecycle statuses are grouped into six stage columns. Dragging a
 * card to another column emits `status-change` with that column's default
 * status — the parent performs the API call (optimistic update + revert on
 * failure). The card's own badge still shows the exact sub-status, which
 * remains editable through the Edit modal, so no information is lost.
 *
 * Native HTML5 drag & drop only — no dependencies. (Mouse-only by design;
 * keyboard/touch users keep the existing Edit-modal status select.)
 */

const props = defineProps<{
  applications: JobApplication[];
  showArchived: boolean;
}>();

const emit = defineEmits<{
  (e: 'status-change', uuid: string, status: ApplicationStatus): void;
  (e: 'view', uuid: string): void;
  (e: 'edit', uuid: string): void;
}>();

interface BoardColumn {
  key: string;
  label: string;
  statuses: ApplicationStatus[];
  /** Status applied when a card is dropped onto this column. */
  target: ApplicationStatus;
  archivedOnly?: boolean;
}

/** Stage grouping — canonical pipeline order, terminal stages last. */
const BOARD_COLUMNS: BoardColumn[] = [
  { key: 'wishlist', label: 'Wishlist', statuses: ['INTERESTED', 'PREPARING', 'DRAFT'], target: 'INTERESTED' },
  { key: 'applied', label: 'Applied', statuses: ['APPLIED', 'ACKNOWLEDGED', 'NO_RESPONSE'], target: 'APPLIED' },
  { key: 'interview', label: 'Interview', statuses: ['INTERVIEW_SCHEDULED', 'INTERVIEW_COMPLETED', 'TECHNICAL_ASSESSMENT', 'INTERVIEWING'], target: 'INTERVIEW_SCHEDULED' },
  { key: 'offer', label: 'Offer', statuses: ['OFFER', 'ACCEPTED'], target: 'OFFER' },
  { key: 'closed', label: 'Closed', statuses: ['REJECTED', 'WITHDRAWN'], target: 'REJECTED' },
  { key: 'archived', label: 'Archived', statuses: ['ARCHIVED'], target: 'ARCHIVED', archivedOnly: true },
];

const columns = computed(() =>
  BOARD_COLUMNS.filter(col => !col.archivedOnly || props.showArchived));

const cardsIn = (col: BoardColumn): JobApplication[] =>
  props.applications.filter(app => col.statuses.includes(app.status));

const columnCount = (col: BoardColumn): number => cardsIn(col).length;

// --- Drag & drop state ---
const draggedUuid = ref<string | null>(null);
const dragOverKey = ref<string | null>(null);

const onDragStart = (app: JobApplication, event: DragEvent) => {
  draggedUuid.value = app.uuid;
  if (event.dataTransfer) {
    event.dataTransfer.setData('text/plain', app.uuid);
    event.dataTransfer.effectAllowed = 'move';
  }
};

const onDragEnd = () => {
  draggedUuid.value = null;
  dragOverKey.value = null;
};

const onDrop = (col: BoardColumn) => {
  const uuid = draggedUuid.value;
  dragOverKey.value = null;
  draggedUuid.value = null;
  if (!uuid) return;

  const app = props.applications.find(a => a.uuid === uuid);
  if (!app) return;
  // Dropping onto the card's own column is a no-op.
  if (col.statuses.includes(app.status)) return;
  emit('status-change', app.uuid, col.target);
};

/** Sub-status badge (the column already implies the stage). */
const statusBadge = (app: JobApplication) =>
  STATUS_META[app.status] ?? { label: app.status, badge: 'bg-secondary' };

const arrangementLabel = (app: JobApplication) => {
  switch ((app.workArrangement ?? '').toUpperCase()) {
    case 'REMOTE': return 'Remote';
    case 'HYBRID': return 'Hybrid';
    case 'ON_SITE': return 'On-site';
    default: return '';
  }
};
</script>

<template>
  <div data-testid="job-board">
    <div class="kanban-board d-flex gap-3 overflow-auto pb-2">
      <div
        v-for="col in columns"
        :key="col.key"
        class="kanban-column flex-shrink-0"
        :class="{ 'drag-over': dragOverKey === col.key }"
        :data-testid="`board-column-${col.key}`"
        @dragover.prevent
        @dragenter.prevent="dragOverKey = col.key"
        @drop.prevent="onDrop(col)"
      >
        <div class="kanban-column-header d-flex align-items-center justify-content-between mb-2">
          <span class="kanban-column-title text-muted text-uppercase small fw-semibold">
            {{ col.label }}
          </span>
          <span class="badge bg-secondary" :data-testid="`column-count-${col.key}`">
            {{ columnCount(col) }}
          </span>
        </div>

        <div class="kanban-column-body" :data-testid="`column-body-${col.key}`">
          <div
            v-for="app in cardsIn(col)"
            :key="app.uuid"
            class="card kanban-card glass-card mb-2"
            draggable="true"
            data-testid="board-card"
            @dragstart="onDragStart(app, $event)"
            @dragend="onDragEnd"
          >
            <div class="card-body p-2">
              <div class="d-flex justify-content-between align-items-start gap-1">
                <span class="kanban-card-title fw-semibold" :title="app.jobTitle">
                  {{ app.jobTitle }}
                </span>
                <span class="badge flex-shrink-0" :class="statusBadge(app).badge" data-testid="board-card-status">
                  {{ statusBadge(app).label }}
                </span>
              </div>
              <div class="small text-muted text-truncate" :title="app.companyName">
                {{ app.companyName }}
              </div>
              <div class="small text-muted text-truncate">
                <template v-if="app.jobLocation">{{ app.jobLocation }}</template>
                <span v-if="arrangementLabel(app)" class="ms-1">{{ arrangementLabel(app) }}</span>
                <span v-if="app.jobType" class="ms-1">{{ app.jobType }}</span>
              </div>
              <div class="d-flex justify-content-end gap-1 mt-1">
                <button
                  class="btn btn-sm btn-outline-info py-0 px-1"
                  title="View details"
                  data-testid="board-view"
                  @click="emit('view', app.uuid)"
                >
                  <Eye :size="14" />
                </button>
                <button
                  class="btn btn-sm btn-outline-secondary py-0 px-1"
                  title="Edit"
                  data-testid="board-edit"
                  @click="emit('edit', app.uuid)"
                >
                  <Pencil :size="14" />
                </button>
              </div>
            </div>
          </div>

          <div v-if="columnCount(col) === 0" class="kanban-empty small text-muted text-center py-3">
            Nothing here yet
          </div>
        </div>
      </div>
    </div>

    <p class="small text-muted mt-2 mb-0" data-testid="board-hint">
      <Info class="me-1" :size="14" style="vertical-align: -2px;" />
      Drag cards between columns to move them through the pipeline (each column
      sets that stage's default status). The exact sub-status — e.g. Technical
      Assessment vs Interview Scheduled — stays editable via <strong>Edit</strong>.
    </p>
  </div>
</template>

<style scoped>
.kanban-column {
  width: 250px;
  min-height: 140px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px dashed rgba(255, 255, 255, 0.15);
  border-radius: 0.5rem;
  padding: 0.6rem;
  transition: background 0.15s, border-color 0.15s;
}

.kanban-column.drag-over {
  background: rgba(13, 110, 253, 0.12);
  border-color: rgba(13, 110, 253, 0.6);
}

.kanban-column-body {
  min-height: 90px;
}

.kanban-card {
  cursor: grab;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border: 1px rgba(var(--bs-body-color-rgb), 0.25);
  border-style: solid;
}

.kanban-card:active {
  cursor: grabbing;
}

.kanban-card-title {
  font-size: 0.85rem;
  line-height: 1.2;
}

.kanban-empty {
  border: 1px rgba(var(--bs-body-color-rgb), 0.15);
  border-style: dotted;
  border-radius: 0.375rem;
}
</style>
