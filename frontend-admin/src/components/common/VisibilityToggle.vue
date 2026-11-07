<template>
  <!-- FIX: The class is now dynamic to support different sizes -->
  <div :class="['form-check form-switch', switchClass]" title="Toggle Visibility">
    <input
      :checked="visible"
      :disabled="isLoading"
      class="form-check-input"
      role="switch"
      type="checkbox"
      @change.stop="$emit('toggle')"
    >
    <label v-if="isLoading" class="form-check-label">
      <span aria-hidden="true" class="spinner-border spinner-border-sm" role="status"></span>
    </label>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';

const props = defineProps<{
  visible: boolean;
  isLoading?: boolean;
  /** 'md' for standard, 'lg' for large */
  size?: 'md' | 'lg';
}>();

defineEmits<{
  (e: 'toggle'): void;
}>();

// This computed property adds the 'form-switch-lg' class when the size is 'lg'.
const switchClass = computed(() => {
  return props.size === 'lg' ? 'form-switch-lg' : '';
});
</script>

<style scoped>
.form-check-input {
  cursor: pointer;
}

/* ADDED: Styles for the large toggle, ensuring consistency with UserSettingsPage. */
.form-switch.form-switch-lg {
  padding-left: 3.5rem;
}

.form-switch.form-switch-lg .form-check-input {
  width: 3rem;
  height: 1.5rem;
}
</style>
