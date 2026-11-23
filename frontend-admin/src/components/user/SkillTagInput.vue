<template>
  <div>
    <!-- 1. Area for displaying selected skills -->
    <div v-if="modelValue.length > 0" class="mb-2">
      <div class="selected-skills-container p-2 border rounded">
        <!-- Phase 0.28 (C1): selected tags carry the catalogue icon too, so a
             picked skill looks the same before and after selection. -->
        <span v-for="skillName in modelValue" :key="skillName" class="badge skill-tag">
          <i :class="[iconForName(skillName), 'me-1']" aria-hidden="true"></i>
          {{ skillName }}
          <button class="btn-close btn-close-white" type="button"
                  :aria-label="`Remove ${skillName}`"
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
            :key="skill.uuid || skill.name"
            class="badge skill-suggestion-tag"
            type="button"
            @mousedown.prevent="addSkill(nameOf(skill))"
          >
            <i :class="[getIconClass(skill), 'me-2']" aria-hidden="true"></i>
            {{ nameOf(skill) }}
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

/**
 * Phase 0.28 (C1): callers historically passed bare skill NAMES (strings)
 * while this component read `skill.name` / `skill.uuid` / `skill.icon`.
 * A string has none of those, so every suggestion rendered as an EMPTY chip
 * (no icon, no name), clicking one called `addSkill(undefined)` (a no-op) and
 * typing in the box threw inside the filter. Accept BOTH shapes and
 * normalize once, so the picker can never render a blank chip again.
 */
type SkillLike = string | Partial<Skill>;

// --- Props and Emits for v-model compatibility ---
const props = defineProps<{
  /** The array of selected skill names */
  modelValue: string[];
  /** Skills to suggest: full objects (preferred) or bare names (legacy) */
  availableSkills: SkillLike[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void;
}>();

// --- Internal State ---
const searchQuery = ref('');
const showSuggestions = ref(false);
const skillInputRef = ref<HTMLInputElement | null>(null);

// --- Normalization helpers ---
/** Normalizes either accepted shape into a skill object. */
const asSkill = (raw: SkillLike): Partial<Skill> =>
  (typeof raw === 'string' ? {name: raw} : raw ?? {});

/** The display name of a normalized skill ('' when it has none). */
const nameOf = (skill: Partial<Skill>): string => skill.name ?? '';

/** Suggestions as normalized, name-bearing objects. */
const normalizedSkills = computed<Partial<Skill>[]>(() =>
  (props.availableSkills ?? [])
    .map(asSkill)
    .filter(skill => !!nameOf(skill))
);

/** Catalogue icon lookup for a bare selected name (falls back to the default). */
const iconForName = (skillName: string): string => {
  const match = normalizedSkills.value.find(skill => nameOf(skill) === skillName);
  return getIconClass(match ?? {name: skillName});
};

// --- Computed Properties ---
const filteredSkills = computed<Partial<Skill>[]>(() => {
  const selected = new Set(props.modelValue);
  const available = normalizedSkills.value.filter(skill => !selected.has(nameOf(skill)));

  const query = searchQuery.value.trim().toLowerCase();
  if (!query) {
    return available;
  }
  return available.filter(skill => nameOf(skill).toLowerCase().includes(query));
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
    addSkill(nameOf(filteredSkills.value[0]));
  } else if (searchQuery.value.trim()) {
    // No catalogue match: treat the typed text as a free-text skill name so
    // Enter still adds what the user typed instead of silently doing nothing.
    addSkill(searchQuery.value.trim());
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

.skill-tag i {
  font-size: 1em;
  line-height: 1;
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
