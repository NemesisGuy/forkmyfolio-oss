<template>
  <div ref="modalRef" aria-hidden="true" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div v-if="skill" class="modal-content glass-modal">
        <div class="modal-header">
          <h5 class="modal-title d-flex align-items-center">
            <i :class="getIconClass(skill)" class="me-3" style="font-size: 1.5rem;"></i>
            {{ skill.name }}
          </h5>
          <!-- FIX: Removed btn-close-white to make the icon theme-aware -->
          <button aria-label="Close" class="btn-close" data-bs-dismiss="modal" type="button"></button>
        </div>
        <div class="modal-body">
          <p v-if="skill.description" class="glass-description">{{ skill.description }}</p>
          <p v-else class="glass-description fst-italic">No detailed description available for this
            skill.</p>

          <hr class="my-3" style="border-color: var(--glass-border-hover);">

          <div class="d-flex justify-content-between align-items-center">
            <div>
              <strong class="glass-subtitle">Category:</strong>
              <span class="badge bg-secondary ms-2">{{ skill.category || 'General' }}</span>
            </div>
            <div>
              <strong class="glass-subtitle">Proficiency:</strong>
              <span :class="['badge', levelBadgeClass, 'ms-2']">{{ skill.level }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline-secondary" data-bs-dismiss="modal" type="button">Close
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue';
import {Modal} from 'bootstrap';
import {getIconClass} from '@/services/iconService.js';
import type { Skill } from '@/types';

const props = defineProps<{
  /**
   * The skill object to display. The modal is shown when this prop is not null.
   */
  skill: Skill | null;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value);
    // FIX: Use 'hide.bs.modal' to fire the close event earlier, preventing a focus race condition.
    modalRef.value.addEventListener('hide.bs.modal', () => emit('close'));
  }
});

watch(() => props.skill, (newSkill) => {
  if (newSkill) {
    modalInstance?.show();
  } else {
    modalInstance?.hide();
  }
});

const levelBadgeClass = computed(() => {
  if (!props.skill || !props.skill.level) return 'text-bg-secondary';
  switch (props.skill.level.toUpperCase()) {
    case 'EXPERT': return 'text-bg-success';
    case 'ADVANCED': return 'text-bg-info';
    case 'INTERMEDIATE': return 'text-bg-primary';
    case 'BEGINNER': return 'text-bg-warning';
    default: return 'text-bg-secondary';
  }
});
</script>
