<template>
  <div ref="modalRef" aria-hidden="true" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div v-if="experience" class="modal-content glass-modal">
        <div class="modal-header">
          <div class="d-flex align-items-center">
            <img v-if="experience.companyLogoUrl" :alt="`${experience.companyName} Logo`"
                 :src="experience.companyLogoUrl" class="company-logo me-3">
            <div v-else class="company-logo-placeholder me-3"><Building2 /></div>
            <div>
              <h5 class="modal-title">{{ experience.jobTitle }}</h5>
              <h6 class="modal-subtitle text-muted">{{ experience.companyName }}</h6>
            </div>
          </div>
          <!-- FIX: Removed btn-close-white to make the icon theme-aware -->
          <button aria-label="Close" class="btn-close" data-bs-dismiss="modal" type="button"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <span class="badge bg-secondary me-2">{{ experience.employmentType?.replace('_', '-') }}</span>
            <span class="badge bg-secondary">{{ experience.locationType?.replace('_', ' ') }}</span>
          </div>

          <!-- Phase 7.4 §10 (self-XSS hardening): text, not HTML -->
          <p v-if="experience.description" class="glass-description">{{ experience.description }}</p>

          <div v-if="experience.achievements" class="mt-4">
            <h6 class="achievements-title">Key Achievements</h6>
            <div class="achievements-text">{{ experience.achievements }}</div>
          </div>

          <div v-if="experience.skills && experience.skills.length > 0" class="mt-4">
            <h6 class="skills-title">Skills Used</h6>
            <div class="d-flex flex-wrap gap-2">
              <SkillBadge
                v-for="skill in experience.skills"
                :key="skill.skillId"
                :skill="skill"
              />
            </div>
          </div>
        </div>
        <div class="modal-footer justify-content-between">
          <div class="text-muted small">
            {{ formatDate(experience.startDate) }} - {{ experience.endDate ? formatDate(experience.endDate) : 'Present' }}
          </div>
          <button class="btn btn-outline-secondary" data-bs-dismiss="modal" type="button">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref, watch} from 'vue';
import {Modal} from 'bootstrap';
import SkillBadge from '@/components/common/SkillBadge.vue';
import {formatDisplayDate as formatDate} from '@/utils/dateUtils.js';
import type { Experience } from '@/types';
import { Building2 } from 'lucide-vue-next';

const props = defineProps<{
  /**
   * The experience object to display. The modal is shown when this prop is not null.
   */
  experience: Experience | null;
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

watch(() => props.experience, (newExp) => {
  if (newExp) {
    modalInstance?.show();
  } else {
    modalInstance?.hide();
  }
});
</script>

<style scoped>
.company-logo, .company-logo-placeholder {
  width: 50px;
  height: 50px;
  border-radius: 0.5rem;
  object-fit: contain;
  flex-shrink: 0;
  background-color: rgba(var(--bs-body-color-rgb), 0.05);
  border: 1px solid var(--glass-border);
}
.company-logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: var(--glass-text-secondary);
}
.achievements-text {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
