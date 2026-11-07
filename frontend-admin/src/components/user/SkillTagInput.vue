<template>
  <div>
    <!-- 1. Area for displaying selected skills -->
    <div v-if="modelValue.length > 0" class="mb-2">
      <div class="selected-skills-container p-2 border rounded">
        <span v-for="skillName in modelValue" :key="skillName" class="badge skill-tag">
          {{ skillName }}
          <button class="btn-close btn-close-white" type="button"
                  @click.stop="removeSkill(skillName)"></button>
        </span>
      </div>
    </div>

    <!-- 2. Area for searching and adding new skills -->
    <div class="position-relative">
      <input
        ref="skillInputRef"
        v-model="searchQuery"
        class="form-control"
        placeholder="Type to find skills..."
        type="text"
        @blur="hideSuggestions"
        @focus="showSuggestions = true"
        @keydown.enter.prevent="selectTopSuggestion"
        @keydown.backspace="handleBackspace"
      />
      <!-- Suggestions Dropdown -->
      <div v-if="showSuggestions && filteredSkills.length > 0" class="skill-suggestions-dropdown">
        <div class="p-2 d-flex flex-wrap gap-2">
          <button
            v-for="skill in filteredSkills"
            :key="skill.uuid"
            class="badge skill-suggestion-tag"
            type="button"
            @mousedown.prevent="addSkill(skill.name)"
          >
            <i :class="[getIconClass(skill), 'me-2']"></i>
            {{ skill.name }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {getIconClass} from '@/services/iconService.js';
import type { Skill } from '@/types';

// --- Props and Emits for v-model compatibility ---
const props = defineProps<{
  /** This is the array of selected skill names */
  modelValue: string[];
  /** This is the full list of skills to suggest from */
  availableSkills: Skill[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void;
}>();

// --- Internal State ---
const searchQuery = ref('');
const showSuggestions = ref(false);
const skillInputRef = ref<HTMLInputElement | null>(null);

// --- Computed Properties ---
const filteredSkills = computed(() => {
  const selected = new Set(props.modelValue);
  if (!searchQuery.value) {
    // Show available skills that aren't yet selected
    return props.availableSkills.filter(skill => !selected.has(skill.name));
  }
  const query = searchQuery.value.toLowerCase();
  // Filter skills that include the search query AND are not already selected
  return props.availableSkills.filter(skill =>
    !selected.has(skill.name) &&
    skill.name.toLowerCase().includes(query)
  );
});

// --- Methods ---
const addSkill = (skillName: string) => {
  if (skillName && !props.modelValue.includes(skillName)) {
    // Emit an update event with a new array, which is the standard for v-model on arrays
    emit('update:modelValue', [...props.modelValue, skillName]);
  }
  searchQuery.value = ''; // Clear input after adding
  skillInputRef.value?.focus(); // Keep focus on the input
};

const removeSkill = (skillNameToRemove: string) => {
  emit('update:modelValue', props.modelValue.filter(name => name !== skillNameToRemove));
};

const selectTopSuggestion = () => {
  if (filteredSkills.value.length > 0) {
    addSkill(filteredSkills.value[0].name);
  }
};

const handleBackspace = () => {
  if (searchQuery.value === '' && props.modelValue.length > 0) {
    removeSkill(props.modelValue[props.modelValue.length - 1]);
  }
};

const hideSuggestions = () => {
  setTimeout(() => {
    showSuggestions.value = false;
  }, 200);
};
</script>

<style scoped>
/* All the skill input styles from the original file go here */
.selected-skills-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  min-height: 40px;
  background-color: rgba(var(--bs-tertiary-bg-rgb), 0.5);
}

.skill-tag {
  background-color: var(--bs-primary);
  color: white;
  padding: 0.3em 0.6em;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9em;
}

.skill-tag .btn-close {
  width: 0.5em;
  height: 0.5em;
  padding: 0;
  filter: brightness(0) invert(1);
}

.skill-suggestions-dropdown {
  position: absolute;
  width: 100%;
  z-index: 10;
  max-height: 200px;
  overflow-y: auto;
  background-color: var(--bs-body-bg);
  border: 1px solid var(--bs-border-color);
  border-top: none;
  border-radius: 0 0 var(--bs-border-radius) var(--bs-border-radius);
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
}

.skill-suggestion-tag {
  background-color: rgba(var(--bs-primary-rgb), 0.15);
  color: var(--bs-primary);
  border: 1px solid transparent;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
}

.skill-suggestion-tag:hover {
  background-color: var(--bs-primary);
  color: white;
  border-color: var(--bs-primary);
}

.skill-suggestion-tag i {
  font-size: 1.1em;
  line-height: 1;
}
</style>
